# 右上角头像信息展示 + 裁剪式头像修改窗口 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在右上角头像旁展示 姓名 + 角色徽章、下拉菜单增加用户信息面板；将头像修改窗口重写为 vue-cropper 裁剪式（1:1 裁剪 + 实时预览 + 确认上传）。纯前端改动，不动后端契约。

**Architecture:** 三处修改——`CommonHeader.vue`（信息展示 + 数据拉取）、`avatarShow.vue`（裁剪式重写）、`api/index.js`（新增 `fetchUserBasicInfo`）。数据来源：登录态 `TaskUser`（userId/userType/Authorization）+ 现有 `GET /user/getUserInfo/{id}`（username/creditScore）。新增依赖 `vue-cropper`（0.6.5，兼容 Vue 2，CSS 已内联无需单独引入）。

**Tech Stack:** Vue 2.6 / Element UI 2.15 / vue-cropper 0.6.5 / vue-cli 5 / less

**验证方式：** 前端无单测基建，每步用 `npm run build` 做构建验证；运行时验收（裁剪/上传/刷新持久化）由 Business Validator 执行（本仓库协作约定：Coding-Agent 不预跑验收用例）。

---

## 关键事实（实现者必读，避免踩坑）

1. **前后端成功码**：`{code: 1, msg, data}`，`code=1` 成功、`code=0` 失败。常量在 `web/src/constants/http.js` 的 `SUCCESS_CODE = 1`。
2. **现有 `getUserInfo` 不可复用**：`web/src/api/index.js` 的 `getUserInfo` 指向 `/userInfo/{id}`（实名信息接口）。本次新增 `fetchUserBasicInfo` 指向 `/user/getUserInfo/{id}`（`UsersController`，返回完整 `Users` 含 `username`/`creditScore`）。
3. **vue-cropper 0.6.5**：`import { VueCropper } from 'vue-cropper'` 即可用（UMD 命名导出 `VueCropper`）；CSS 已内联进 `dist/index.js`，**不要** `import 'vue-cropper/dist/index.css'`（0.6.5 包内无此文件，会构建报错）。裁剪器方法 `getCropBlob(cb)` / `getCropData(cb)` / `rotateLeft()` / `rotateRight()` / `changeScale(num)`；实时预览用 `@real-time` 事件，payload `data.url` 为裁剪结果 URL。内置 toBlob polyfill，旧浏览器可用。
4. **`avatarSrc` 刷新丢失修复**：`CommonHeader.mounted` 目前不读 `localStorage.avatarSrc`（读取代码被注释）。需改为 `this.avatarSrc = localStorage.getItem('avatarSrc') || ''`。
5. **上传成功返回**：`uploadAvatar` POST `/img/uploadAvatar`，成功 `data.data` 是**不含协议**的路径串，前端需拼 `'http://' + data.data`。
6. **工作目录陷阱**：`web/` 目录下 `npm` 命令直接在 `web/` 跑；`git` 命令必须回到仓库根 `D:\workspace-dev\java\campus-errand`。

---

### Task 1: 安装 vue-cropper 依赖

**Files:**
- Modify: `web/package.json`
- Modify: `web/package-lock.json`

- [ ] **Step 1: 安装依赖**

在 `web/` 目录执行：

```bash
npm install vue-cropper
```

期望：安装 `vue-cropper@0.6.5`（latest tag 即 0.6.5，Vue 2 兼容），`package.json` 的 `dependencies` 出现 `"vue-cropper": "^0.6.5"`，`package-lock.json` 更新。

- [ ] **Step 2: 提交**

```bash
cd D:\workspace-dev\java\campus-errand
git add web/package.json web/package-lock.json
git commit -m "chore: 新增 vue-cropper 依赖用于裁剪式头像修改"
```

---

### Task 2: 新增 fetchUserBasicInfo 接口封装

**Files:**
- Modify: `web/src/api/index.js`（在现有 `getUserInfo` 函数之后插入）

- [ ] **Step 1: 在 `web/src/api/index.js` 的 `getUserInfo` 函数（约 88-90 行）后插入新函数**

找到：

```js
export const getUserInfo = (id) => {
    return http.get('/userInfo/' + id)
}
```

在其后追加：

```js
// 获取用户基础资料（UsersController：/user/getUserInfo/{id}，含 username/creditScore；区别于上面的 /userInfo 实名信息接口）
export const fetchUserBasicInfo = (id) => {
    return http.get('/user/getUserInfo/' + id)
}
```

- [ ] **Step 2: 构建验证**

```bash
cd D:\workspace-dev\java\campus-errand\web
npm run build
```

期望：`npm run build` 以退出码 0 完成（`Compiled successfully`），无报错。

- [ ] **Step 3: 提交**

```bash
cd D:\workspace-dev\java\campus-errand
git add web/src/api/index.js
git commit -m "feat: 新增 fetchUserBasicInfo 接口封装（/user/getUserInfo）"
```

---

### Task 3: 头部头像区信息展示（CommonHeader.vue）

**Files:**
- Modify: `web/src/components/CommonHeader.vue`

- [ ] **Step 1: 修改 script 的 import**

将现有的两行 import：

```js
  import { logout } from '@/api';
  import { uploadAvatar } from '@/api';
```

替换为一行合并 + 新增 SUCCESS_CODE import：

```js
  import { logout, uploadAvatar, fetchUserBasicInfo } from '@/api';
  import { SUCCESS_CODE } from '@/constants/http';
```

- [ ] **Step 2: 修改 data()，新增 `defaultAvatar` 与 `userProfile`**

现有 `data()` 中 `avatarSrc: ''` 所在的对象，新增两个字段：

```js
    data() {
      return {
        dialogAvatarVisible: false,
        imageUrl: '',
        initialImageSrc: '',
        avatarSrc: '',
        defaultAvatar: require('../assets/avatar.jpg'),
        userProfile: null,
        dialogWidth: '600px',
        dialogNoticeVisible: false,
        userId: '',
      }

    },
```

- [ ] **Step 3: 替换头像下拉的 template 块**

将现 template 中的 `el-dropdown` 整块（`<el-dropdown trigger="click" class="avatar-dropdown">...` 至 `</el-dropdown>`）替换为：

```html
      <el-dropdown trigger="click" class="avatar-dropdown">
        <div class="avatar-wrapper">
          <img class="userIcon" :src="avatarSrc || defaultAvatar" alt="用户">
          <div class="avatar-info">
            <span class="avatar-name">{{ displayName }}</span>
            <span class="role-tag" :class="roleInfo.cls">{{ roleInfo.label }}</span>
          </div>
          <i class="el-icon-caret-bottom"></i>
        </div>
        <el-dropdown-menu slot="dropdown" class="user-dropdown">
          <div class="user-panel">
            <img class="panel-avatar" :src="avatarSrc || defaultAvatar" alt="用户">
            <div class="panel-meta">
              <div class="panel-name-row">
                <span class="panel-name">{{ displayName }}</span>
                <span class="role-tag" :class="roleInfo.cls">{{ roleInfo.label }}</span>
              </div>
              <div class="panel-row"><span class="panel-label">用户ID</span>{{ userId || '--' }}</div>
              <div class="panel-row"><span class="panel-label">实名状态</span>{{ authDesc || '未认证' }}</div>
              <div class="panel-row"><span class="panel-label">信用分</span>{{ creditDisplay }}</div>
            </div>
          </div>
          <el-dropdown-item @click.native="dialogAvatarVisible = true">
            <i class="el-icon-user"></i>
            修改头像
          </el-dropdown-item>
          <el-dropdown-item divided @click.native="handleLogout">
            <i class="el-icon-switch-button"></i>
            退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
```

- [ ] **Step 4: 替换 mounted()，读取 localStorage 头像并拉取用户资料**

将现有 `mounted()`：

```js
    mounted() {
      // console.log(this.tags, 'tags')
      const TaskUser = localStorage.getItem('TaskUser')
      if (TaskUser) {
        this.userId = JSON.parse(TaskUser).userId;
      }
      const parsedUser = JSON.parse(TaskUser);
      // this.avatarSrc = parsedUser.avatarSrc;
    }
```

替换为：

```js
    mounted() {
      let taskUser = {};
      try {
        taskUser = JSON.parse(localStorage.getItem('TaskUser') || '{}') || {};
      } catch (e) {
        taskUser = {};
      }
      this.userId = taskUser.userId || this.$store.state.userInfo.userId || '';
      // 修复：上传的头像在刷新后丢失（此前读取 localStorage 的代码被注释）
      this.avatarSrc = localStorage.getItem('avatarSrc') || '';
      if (this.userId) {
        this.loadUserProfile(this.userId);
      }
    }
```

- [ ] **Step 5: 在 methods 中新增 `loadUserProfile` 方法**

在 `methods` 对象中（如 `handleMenu()` 之前）新增（需在 Step 1 把 `getUserInfo` 一并 import）：

```js
      async loadUserProfile(userId) {
        try {
          const res = await fetchUserBasicInfo(userId);
          if (res.data && res.data.code === SUCCESS_CODE && res.data.data) {
            const u = res.data.data;
            this.userProfile = { name: u.username || '', credit: u.creditScore, userRole: '' };
          }
        } catch (e) {
          console.warn('加载用户资料失败', e);
        }
        // users.Role 仅 USER/ADMIN，学生/教师来自 usersinfo.UserRole（小写 student/teacher，仅已实名用户存在）
        try {
          const r = await getUserInfo(userId);
          if (r.data && r.data.code === SUCCESS_CODE && r.data.data && r.data.data.userRole) {
            this.userProfile.userRole = r.data.data.userRole;
          }
        } catch (e) {
          // 非实名/管理员无 usersinfo 记录，userRole 保持为空
        }
      },
```

> **import 说明**：Step 1 的 `import { logout, uploadAvatar, fetchUserBasicInfo } from '@/api';` 需改为 `import { logout, uploadAvatar, fetchUserBasicInfo, getUserInfo } from '@/api';`（`getUserInfo` 已存在，指向 `/userInfo/{id}`，用于取 `userRole`）。

- [ ] **Step 6: 在 computed 中新增角色映射与展示字段**

现有 `computed` 里 `...mapState({ tags: ... })` 之后，新增（`users.Role` 枚举仅 `USER`/`ADMIN`，学生/教师取 `userProfile.userRole`）：

```js
      roleInfo() {
        const role = this.$store.state.userInfo.userType || '';
        if (role === 'ADMIN') {
          return { label: '管理员', cls: 'role-admin' };
        }
        const userRole = (this.userProfile && this.userProfile.userRole) || '';
        const map = {
          student: { label: '学生', cls: 'role-student' },
          teacher: { label: '教师', cls: 'role-teacher' },
        };
        return map[userRole] || { label: '普通用户', cls: 'role-default' };
      },
      displayName() {
        return (this.userProfile && this.userProfile.name) || '用户';
      },
      creditDisplay() {
        const c = this.userProfile ? this.userProfile.credit : null;
        return (c !== null && c !== undefined) ? String(c) : '--';
      },
      authDesc() {
        try {
          return (JSON.parse(localStorage.getItem('TaskUser') || '{}')).Authorization || '';
        } catch (e) {
          return '';
        }
      },
```

- [ ] **Step 7: 新增样式（头像旁信息列 + 下拉信息面板 + 角色徽章）**

在 `<style lang="less" scoped>` 内、`.avatar-dropdown` 区块的 `.avatar-wrapper` 内部，新增 `.avatar-info` 样式；并在 `.r-content` 之后新增 `.role-tag`、`.user-panel` 样式。具体在 `.avatar-wrapper` 的 `.userIcon` 规则之后追加：

```less
          .avatar-info {
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            margin-left: 10px;
            line-height: 1.2;

            .avatar-name {
              font-size: 14px;
              font-weight: 600;
              color: #303133;
              max-width: 120px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }
          }
```

**关键：`.role-tag` 与 `.user-panel` 必须放在 `<style lang="less" scoped>` 的顶层**（即 `.header-container` 的同级兄弟，不能嵌套在 `.header-container` 内部）。原因：Element UI 的 el-dropdown 默认 `appendToBody` 会把下拉菜单 DOM 移到 `document.body`，若选择器编译为 `.header-container .user-panel[data-v-x]`，移动后祖先 `.header-container` 不存在，面板样式全部失效。顶层编译为 `.user-panel[data-v-x]`（元素自带 scope 属性并随 DOM 移动保留），即可正常生效。故在 `.header-container` 块关闭后、`@media` 之前，新增：

```less
    .role-tag {
      display: inline-block;
      margin-top: 2px;
      padding: 0 6px;
      font-size: 11px;
      line-height: 18px;
      border-radius: 3px;

      &.role-admin { color: #b88230; background: #fdf6ec; border: 1px solid #f3d19e; }
      &.role-teacher { color: #2d8f6f; background: #e6f7f0; border: 1px solid #b7e3d0; }
      &.role-student { color: #3370ff; background: #ecf3ff; border: 1px solid #b9d0ff; }
      &.role-default { color: #909399; background: #f4f4f5; border: 1px solid #dcdce0; }
    }

    .user-panel {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      min-width: 220px;
      border-bottom: 1px solid #ebeef5;

      .panel-avatar {
        width: 48px;
        height: 48px;
        border-radius: 50%;
        object-fit: cover;
        margin-right: 12px;
        border: 2px solid #fff;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }

      .panel-meta {
        flex: 1;
        min-width: 0;
      }

      .panel-name-row {
        display: flex;
        align-items: center;
        gap: 6px;
      }

      .panel-name {
        font-size: 15px;
        font-weight: 600;
        color: #303133;
      }

      .panel-row {
        font-size: 12px;
        color: #909399;
        margin-top: 4px;

        .panel-label {
          color: #c0c4cc;
          margin-right: 6px;
        }
      }
    }
```

- [ ] **Step 8: 移动端隐藏姓名列**

在现有 `@media screen and (max-width: 768px)` 块的 `.avatar-wrapper .userIcon` 规则之后追加：

```less
          .avatar-wrapper {
            .avatar-info {
              display: none;
            }
          }
```

- [ ] **Step 9: 构建验证**

```bash
cd D:\workspace-dev\java\campus-errand\web
npm run build
```

期望：退出码 0，`Compiled successfully`，无编译/ESLint 报错。

- [ ] **Step 10: 提交**

```bash
cd D:\workspace-dev\java\campus-errand
git add web/src/components/CommonHeader.vue
git commit -m "feat: 右上角头像区展示姓名/角色徽章，下拉新增用户信息面板，修复头像刷新丢失"
```

---

### Task 4: 重写头像修改窗口（avatarShow.vue，vue-cropper 裁剪式）

**Files:**
- Rewrite: `web/src/components/avatarShow.vue`

> **审查后修正（commit `904c51a`，以下方说明为准，本任务下方的旧代码块已被取代）**：
> 1. **实时预览不能用 `@real-time` 的 `data.url`**——vue-cropper 0.6.5 中它是原图。改为 README 模式：保存整个载荷到 `previews`，用 `:style="previews.div"` 套 `:style="previews.img"`，外层 `previewScaleStyle`（`width/height=previews.w`，`transform: scale(96/previews.w)`，origin top-left）缩放进 96px 圆形（`overflow:hidden`）。
> 2. **确认上传静默失效兜底**：`getCropBlob` 仅在裁剪布局就绪后回调。新增 `cropReady`（`@img-load` 置 true / `@img-load-error` 置 false 并报错），确认按钮 `:disabled="!cropReady || uploading"`；`confirmUpload` 开头同步置 `uploading=true`，加 3s 超时 `settled` 守卫，未回调时提示「裁剪生成超时」。
> 3. **损坏/伪装图片**：绑定 `@img-load-error`；`onFileChange` 开头重置 `previews`/`cropReady`；`fileToDataUrl` 包 try/catch 提示「图片读取失败」。
> 修正后实现以 `904c51a` 为准（模板含 `@img-load`/`@img-load-error`、`previews`/`cropReady`/`uploading` 数据字段、`circleStyle`/`previewScaleStyle` computed）。

- [ ] **Step 1: 用以下内容整体覆盖 `web/src/components/avatarShow.vue`**

```vue
<template>
  <div class="avatar-show">
    <div class="cropper-area">
      <vue-cropper
        v-if="cropImg"
        ref="cropper"
        class="cropper"
        :img="cropImg"
        :output-type="'jpeg'"
        :auto-crop="true"
        :fixed="true"
        :fixed-number="[1, 1]"
        :center-box="true"
        :info="false"
        :can-move="true"
        :can-move-box="true"
        :can-scale="true"
        :auto-crop-width="240"
        :auto-crop-height="240"
        @real-time="onRealTime"
      />
      <el-empty v-else description="请选择一张图片" />
      <div v-if="cropImg" class="crop-tools">
        <el-button size="mini" @click="rotateLeft">左转</el-button>
        <el-button size="mini" @click="rotateRight">右转</el-button>
        <el-button size="mini" @click="zoomIn">放大</el-button>
        <el-button size="mini" @click="zoomOut">缩小</el-button>
      </div>
    </div>

    <div class="side-area">
      <div class="preview-wrap">
        <div class="preview-circle" :style="previewStyle"></div>
        <p class="preview-label">头像预览</p>
      </div>
      <div class="actions">
        <el-button type="primary" @click="handleUpload">{{ cropImg ? '重新选择' : '选择图片' }}</el-button>
        <el-button type="success" :disabled="!cropImg || uploading" :loading="uploading" @click="confirmUpload">
          确认上传
        </el-button>
      </div>
      <div class="tips">
        <p>支持 JPG / PNG / GIF 格式，大小不超过 5MB。</p>
        <p>拖动、缩放或旋转图片，裁剪框外区域将被裁掉。</p>
      </div>
    </div>

    <input ref="fileInput" type="file" accept="image/*" style="display: none;" @change="onFileChange" />
  </div>
</template>

<script>
import { VueCropper } from 'vue-cropper';
import { uploadAvatar } from '@/api';
import { SUCCESS_CODE } from '@/constants/http';

export default {
  name: 'avatarShow',
  components: { VueCropper },
  props: {
    initialSrc: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      imageSrc: this.initialSrc,
      cropImg: '',
      previewUrl: '',
      uploading: false,
    };
  },
  computed: {
    previewStyle() {
      const url = this.previewUrl || this.imageSrc;
      return {
        backgroundImage: url ? `url(${url})` : 'none',
      };
    },
  },
  methods: {
    onRealTime(data) {
      // vue-cropper 实时裁剪事件，payload.data.url 为裁剪结果 base64
      this.previewUrl = data.url || '';
    },
    handleUpload() {
      this.$refs.fileInput.click();
    },
    async onFileChange(event) {
      const file = event.target.files[0];
      event.target.value = ''; // 允许连续选择同一文件
      if (!file) return;
      if (!this.validateFileType(file)) return;
      if (!this.validateFileSize(file)) return;
      const dataUrl = await this.fileToDataUrl(file);
      this.cropImg = dataUrl;
    },
    fileToDataUrl(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = reject;
        reader.readAsDataURL(file);
      });
    },
    validateFileType(file) {
      if (!/\.(jpg|jpeg|png|gif)$/i.test(file.name)) {
        this.$message.error('只允许上传JPG、PNG、GIF格式的图片');
        return false;
      }
      return true;
    },
    validateFileSize(file) {
      if (file.size / (1024 * 1024) > 5) {
        this.$message.error('图片大小不能超过5MB');
        return false;
      }
      return true;
    },
    rotateLeft() {
      this.$refs.cropper.rotateLeft();
    },
    rotateRight() {
      this.$refs.cropper.rotateRight();
    },
    zoomIn() {
      this.$refs.cropper.changeScale(0.1);
    },
    zoomOut() {
      this.$refs.cropper.changeScale(-0.1);
    },
    confirmUpload() {
      if (!this.$refs.cropper) return;
      this.$refs.cropper.getCropBlob((blob) => {
        if (!blob) {
          this.$message.error('裁剪失败，请重试');
          return;
        }
        const file = new File([blob], 'avatar.jpg', { type: 'image/jpeg' });
        this.uploading = true;
        uploadAvatar(file)
          .then((result) => {
            if (result.data && result.data.code === SUCCESS_CODE && result.data.data) {
              const url = 'http://' + result.data.data;
              this.imageSrc = url;
              this.$store.commit('updatedAvatarSrc', url);
              this.$message.success('头像修改成功');
            } else {
              this.$message.error((result.data && result.data.msg) || '上传失败');
            }
          })
          .catch(() => {
            this.$message.error('网络错误，上传失败');
          })
          .finally(() => {
            this.uploading = false;
          });
      });
    },
  },
};
</script>

<style lang="less" scoped>
.avatar-show {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.cropper-area {
  flex: 1;
  min-width: 260px;

  .cropper {
    width: 300px;
    height: 300px;
    border-radius: 8px;
    overflow: hidden;
    background: #f5f7fa;
    border: 1px solid #ebeef5;
  }

  .crop-tools {
    margin-top: 12px;
    text-align: center;
  }
}

.side-area {
  width: 220px;

  .preview-wrap {
    text-align: center;

    .preview-circle {
      width: 96px;
      height: 96px;
      margin: 0 auto;
      border-radius: 50%;
      background-size: cover;
      background-position: center;
      background-repeat: no-repeat;
      border: 3px solid #fff;
      box-shadow: 0 2px 10px rgba(0, 0, 0, 0.12);
    }

    .preview-label {
      margin-top: 8px;
      font-size: 12px;
      color: #909399;
    }
  }

  .actions {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: 16px;
  }

  .tips {
    margin-top: 16px;
    padding: 10px 12px;
    background: #f5f7fa;
    border-radius: 6px;

    p {
      margin-bottom: 4px;
      font-size: 12px;
      line-height: 1.5;
      color: #909399;
    }
  }
}

@media screen and (max-width: 600px) {
  .avatar-show {
    flex-direction: column;
  }

  .side-area {
    width: 100%;
  }
}
</style>
```

- [ ] **Step 2: 构建验证**

```bash
cd D:\workspace-dev\java\campus-errand\web
npm run build
```

期望：退出码 0，`Compiled successfully`，无报错。

> 若构建因 vue-cropper 报 transpile 类错误（0.6.5 为 webpack4 UMD，理论可直接打包），在 `web/vue.config.js` 的 `transpileDependencies` 追加 `'vue-cropper'` 后重跑。若仍失败，停下并回报，勿擅自改方案。

- [ ] **Step 3: 提交**

```bash
cd D:\workspace-dev\java\campus-errand
git add web/src/components/avatarShow.vue
git commit -m "feat: 头像修改窗口改用 vue-cropper 裁剪式（1:1 裁剪/旋转/缩放/实时预览/确认上传）"
```

---

### Task 5: 汇总验证

- [ ] **Step 1: 全量构建**

```bash
cd D:\workspace-dev\java\campus-errand\web
npm run build
```

期望：退出码 0，无报错。若 dev server 已启动可同时确认无热更新报错。

- [ ] **Step 2: 检查 git 状态干净**

```bash
cd D:\workspace-dev\java\campus-errand
git status
```

期望：仅设计文档/计划文档为未跟踪或已提交，无未提交的代码改动。

- [ ] **Step 3: 记录交付说明（不自行跑验收）**

按仓库协作约定，运行时验收由 Business Validator 执行。交付时向用户说明以下验收要点（不在本会话执行）：
- 三种身份（zhangsan 学生 / lisiyuan 教师 / majiaqi 管理员）登录：头像旁显示正确姓名与角色徽章，配色正确。
- 下拉菜单顶部信息面板展示 头像/姓名/角色/用户ID/实名状态/信用分。
- 修改头像：选图 → 1:1 裁剪 → 实时预览 → 旋转/缩放 → 确认上传 → 头部头像更新，**刷新页面后仍保持**。
- 上传失败场景（超 5MB、非图片格式、网络断开）均有提示且不崩溃。

---

## 自检记录

- **Spec 覆盖**：Spec 三块（头像信息展示 / 裁剪式窗口 / 头像刷新修复）分别对应 Task 3 / Task 4 / Task 3 Step 4；`fetchUserBasicInfo` 对应 Task 2；vue-cropper 依赖对应 Task 1。Spec「移除正方形建议校验」「cropmove 节流预览」在 Task 4 通过移除 `validateImageDimensions` + `@real-time`（裁剪器内置节流）落实。
- **占位符扫描**：无 TBD/TODO，代码块均为完整可粘贴内容。
- **类型/命名一致性**：`fetchUserBasicInfo`（api）→ 在 `CommonHeader` import 并调用；`SUCCESS_CODE` 两处一致；vue-cropper 方法名 `getCropBlob/rotateLeft/rotateRight/changeScale` 与 0.6.5 实测导出一致；`roleInfo.cls` 与 `.role-tag` 变体类名一一对应。
