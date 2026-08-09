# 右上角头像信息展示 + 裁剪式头像修改窗口设计

日期：2026-08-09
范围：`web/src/components/CommonHeader.vue`（头像区信息展示）、`web/src/components/avatarShow.vue`（裁剪式重写）、`web/src/api/index.js`（新增一个接口封装）、`web/package.json`（新增依赖 vue-cropper）。**纯前端改动，不动后端接口、不改 API 契约、不重启后端。**

## 背景

- 现状：右上角 `.avatar-wrapper` 仅显示圆形头像 + 下拉箭头；下拉菜单只有「修改头像」「退出登录」两项，无用户信息。
- `avatarShow.vue` 布局破损：硬编码绝对定位（`top: -420px; right: -414px`），400×400 大图 + 上传按钮 + 说明文字，响应式与可维护性差。
- 用户确认方案：
  - 头像修改窗口采用**裁剪式（引入 vue-cropper）**，业界经典流程。
  - 信息展示采用**姓名 + 身份 + 下拉完整信息**。

## 数据来源（不改后端）

- 登录态已具备：`localStorage.TaskUser` 含 `userId`、`userType`（角色：ADMIN/STUDENT/TEACHER）、`Authorization`（实名状态中文描述）。
- 姓名 + 信用分：头部 `mounted` 调现有 `GET /user/getUserInfo/{userId}`（`UsersController.getUserInfo`，返回完整 `Users` 含 `username` / `creditScore`）。
  - **注意**：`web/src/api/index.js` 现有 `getUserInfo` 指向 `/userInfo/{id}`（实名信息接口），不可复用，需新增一个封装（如 `fetchUserBasicInfo(id)` → `GET /user/getUserInfo/{id}`）。
- 角色 → 中文徽章映射（前端本地常量）：`ADMIN→管理员`、`STUDENT→学生`、`TEACHER→教师`；徽章配色：管理员=金色/红、教师=绿色、学生=蓝色。
- 附带修复：当前头像刷新后丢失（`mounted` 读取 `localStorage.avatarSrc` 的代码被注释）。改为初始化时读取 `localStorage.getItem('avatarSrc') || ''`，让上传头像跨刷新保持。

## 一、头部头像区（CommonHeader.vue）

- `.avatar-wrapper` 内头像旁新增纵向信息列：**姓名**（加粗，14px）+ **角色徽章**（彩色小 `el-tag`）+ 保留下拉箭头。
- 下拉菜单顶部新增**用户信息面板**（分隔线上方）：头像（40px 圆形）、姓名、角色标签、用户 ID、实名状态、信用分；分隔线后保留「修改头像」「退出登录」。
- 信息面板数据在 `mounted` 通过 `fetchUserBasicInfo(userId)` 拉取，挂到组件 `data`（`userProfile`）。
- 实名状态优先用 `TaskUser.Authorization`（无需额外请求）；`fetchUserBasicInfo` 失败时不阻塞头部渲染（静默降级，仅显示姓名/角色兜底为空）。

## 二、头像修改窗口（avatarShow.vue 重写 + vue-cropper）

- 新增依赖：`vue-cropper`（0.6.x，兼容 Vue 2，vue-element-admin 同款）。
- 布局（干净 flex 双栏）：
  - 左栏：`<vue-cropper>` 裁剪画布（固定约 300×300，裁剪框 1:1，支持拖动/缩放/旋转）。
  - 右栏：选择图片按钮、确认上传按钮、**圆形实时预览**、校验提示文案（仅保留类型/大小两条，移除「建议正方形」行）。
- 流程：
  1. 点击选图（隐藏 `<input type=file>`，`accept="image/*"`）→ 复用现有校验（类型 JPG/JPEG/PNG/GIF、大小 ≤5MB）→ 通过后把图片 URL 载入裁剪器。
     - **变更**：现有校验含「长宽比 0.66~1.5」限制，裁剪式下不再需要（用户可自行裁成任意方形），移除该项校验。
  2. 用户调整裁剪框 → 监听 `cropmove`（节流）调用 `refs.cropper.getCropData`（base64）驱动右侧圆形预览；`cropend` 再做一次最终刷新。
  3. 点「确认上传」→ `getCropBlob()` 得 Blob → `new File([blob], 'avatar.jpg', { type: 'image/jpeg' })` → 走现有 `uploadAvatar` 接口（POST `/img/uploadAvatar`）→ 成功后更新 store mutation `updatedAvatarSrc` + `localStorage.avatarSrc`，并通知父组件刷新。
- 弹窗样式沿用现有 `my-dialog` 体系（`CommonHeader.vue` 中 `/deep/` 覆盖：圆角 8px、头部/主体留白）。裁剪器容器适配弹窗宽度，移动端单栏堆叠。

## 数据流

`CommonHeader.mounted` → 读 `TaskUser.userId/userType/Authorization` → `fetchUserBasicInfo(userId)` → 填充 `userProfile`（姓名/信用分）→ 模板渲染头像列 + 下拉信息面板；「修改头像」→ 打开 `avatarShowVue` 对话框 → 裁剪 → 上传 → store + localStorage 更新 → 关闭时 `handleDialogClose` 回读 store 刷新头像。

## 验收

- 不同身份登录（zhangsan / lisiyuan / majiaqi）：头像旁显示对应姓名 + 角色徽章（学生/教师/管理员），配色正确。
- 下拉菜单顶部信息面板展示 头像/姓名/角色/用户ID/实名状态/信用分。
- 上传头像：选图 → 裁剪（1:1）→ 实时预览 → 确认上传 → 头部头像立即更新，**刷新后仍保持**。
- 裁剪窗口布局在 600px 弹窗内正常、无溢出；`npm run build` 通过。
- 回归：未改动任何后端接口契约，TC-032 等后端验收不受影响。

## 范围外

- 不改后端 `UsersController` / `login` 契约（不为登录响应新增 userName）。
- 不引入 `el-upload` 自动上传（保持手动选择 + 手动确认上传流程）。
- 旧浏览器若无 Blob 裁剪支持：确认上传前做一次能力检测并提示，不做降级方案。
