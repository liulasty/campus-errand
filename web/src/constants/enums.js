// web/src/constants/enums.js
// 与后端 com.lz.pojo.Enum 对齐的状态/类型枚举映射（key=dbValue/枚举名，value=中文 webValue）
// 统一为单一数据源：改一处 webValue 全局联动。

// ===== 任务状态（后端 TaskStatus）=====
export const TASK_STATUS = {
  DRAFT: '草稿',
  AUDITING: '审核中',
  AUDIT_FAILED: '审核未通过',
  PENDING_RELEASE: '等待发布',
  ONGOING: '委托发布中',
  ACCEPTED: '已接收',
  COMPLETED: '已完成',
  UNFINISHED: '未完成',
  EXPIRED: '已过期',
  CANCELLED: '已取消'
}

// ===== 接单状态（后端 AcceptStatus）=====
export const ACCEPT_STATUS = {
  UNCHECKED: '未选中',
  CHECKED: '已接收',
  EXPIRED: '已过期',
  PENDING: '待处理',
  CANCEL: '已取消'
}

// ===== 公告状态（后端 AnnouncementStatus）=====
export const ANNOUNCEMENT_STATUS = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  WITHDRAWN: '已撤回'
}

// ===== 实名认证状态（后端 AuthenticationStatus，key=枚举名，查询契约用英文枚举名）=====
export const AUTH_STATUS = {
  UNAUTHORIZED: '未认证',
  AUTHENTICATING: '认证中',
  AUTHENTICATION_FAILED: '认证失败',
  AUTHENTICATED: '认证通过'
}

// ===== 通知类型（后端 NotificationsType）=====
export const NOTIFICATION_TYPE = {
  OWN: '个人信息通知',
  TASK: '委托信息通知',
  SYSTEM: '系统信息通知',
  MARKETING: '营销信息通知'
}

// ===== 履约更新类型（后端 TaskUpdateType）=====
export const TASK_UPDATE_TYPE = {
  AUDITING: '审核',
  PUBLISHED: '发布委托',
  CREATED: '新委托创建',
  RESULT: '委托结果',
  FALLBACK_DRAFT: '回退草稿',
  PROGRESS_UPDATE: '进度更新',
  CONTACTED: '已联系',
  PICKED_UP: '已取件',
  DELIVERED: '已送达',
  AUTO_ADVANCE: '自动推进'
}

// ===== 任务阶段（后端 TaskPhase，仅英文，查询用）=====
export const TASK_PHASE = {
  EDITING_AND_AUDITING: 'EDITING_AND_AUDITING',
  PUBLISHING_AND_EXECUTION: 'PUBLISHING_AND_EXECUTION',
  LIFECYCLE_TERMINATION: 'LIFECYCLE_TERMINATION'
}

// ===== 通用工具 =====

// dbValue 或 webValue → 中文展示（未知值原样返回）
export function enumText(enumMap, value) {
  if (value === null || value === undefined || value === '') return ''
  return enumMap[value] !== undefined ? enumMap[value] : value
}

// webValue 或 dbValue → dbValue（查询/提交参数用）
export function enumDb(enumMap, value) {
  if (value === null || value === undefined || value === '') return ''
  if (enumMap[value] !== undefined) return value
  const db = Object.keys(enumMap).find(k => enumMap[k] === value)
  return db || value
}

// el-select 选项 [{value: dbValue, label: webValue}]，可选「全部」占位
export function enumOptions(enumMap, { empty = false, emptyLabel = '全部' } = {}) {
  const opts = Object.entries(enumMap).map(([value, label]) => ({ value, label }))
  return empty ? [{ value: '', label: emptyLabel }, ...opts] : opts
}
