/**
 * 全局统一时间转换工具（独立文件，不混入 main.js）
 * 兼容：Date 对象 / 毫秒时间戳 / 数字字符串 / ISO 时间字符串。
 * 统一：格式非法或为空时返回占位符（默认 '—'，可按需传 ''）。
 * 用法：
 *   import { formatDateTime } from '@/utils/dateFormat'
 *   import '@/utils/dateFormat'   // 仅引入时自动注册全局过滤器
 */
import Vue from 'vue'

function toDate(value) {
  if (value === null || value === undefined || value === '') return null
  if (value instanceof Date) return isNaN(value.getTime()) ? null : value
  const str = String(value).trim()
  // 纯数字（含负号/小数）按毫秒时间戳解析，否则按日期字符串（ISO 等）解析
  const num = /^-?\d+(\.\d+)?$/.test(str) ? Number(value) : value
  const d = new Date(num)
  return isNaN(d.getTime()) ? null : d
}

const pad = n => (n < 10 ? '0' + n : n)

// yyyy-MM-dd HH:mm[:ss]
export function formatDateTime(value, includeSeconds = false, placeholder = '—') {
  const d = toDate(value)
  if (!d) return placeholder
  const base = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  return includeSeconds ? `${base}:${pad(d.getSeconds())}` : base
}

// yyyy-MM-dd
export function formatDate(value, placeholder = '—') {
  const d = toDate(value)
  if (!d) return placeholder
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// 友好相对时间：刚刚 / N分钟前 / N小时前 / 昨天 / N天前 / yyyy-MM-dd
export function formatRelative(value, placeholder = '—') {
  const d = toDate(value)
  if (!d) return placeholder
  const diff = Date.now() - d.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)}分钟前`
  if (diff < day) return `${Math.floor(diff / hour)}小时前`
  if (diff < 2 * day) return '昨天'
  if (diff < 7 * day) return `${Math.floor(diff / day)}天前`
  return formatDate(value)
}

// 全局过滤器注册：时间处理逻辑整体独立于 main.js
Vue.filter('dateTime', formatDateTime)
Vue.filter('date', formatDate)
Vue.filter('relativeTime', formatRelative)

export default { formatDateTime, formatDate, formatRelative }
