// 履约三节点打卡：节点元数据（dbValue / 展示名 / 图标 / 颜色）
// updateType 在后端枚举序列化后为中文 webValue（如「已联系」），此处兼容 dbValue 与中文两种取值。
export const TASK_NODE_TYPES = [
  { dbValue: 'CONTACTED', label: '已联系', icon: 'el-icon-phone-outline', color: '#409EFF' },
  { dbValue: 'PICKED_UP', label: '已取件', icon: 'el-icon-box', color: '#E6A23C' },
  { dbValue: 'DELIVERED', label: '已送达', icon: 'el-icon-location-outline', color: '#67C23A' }
]

export const TASK_NODE_DBVALUE_SET = new Set(TASK_NODE_TYPES.map(t => t.dbValue))

export function getNodeMeta(updateType) {
  return TASK_NODE_TYPES.find(t => t.dbValue === updateType || t.label === updateType) || null
}

export function isNodeType(updateType) {
  return getNodeMeta(updateType) != null
}

// 演示用的模拟定位
export const MOCK_LOCATION = '120.1124,30.2994'
