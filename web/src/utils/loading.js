import { Loading } from 'element-ui'

let loadingInstance = null
let pendingCount = 0

/**
 * 统一的全局加载遮罩（计数式，多个并发请求共享一个实例）。
 * 页面/操作里调用 showLoading/hideLoading 成对出现即可。
 */
export function showLoading(text = '加载中...') {
    pendingCount++
    if (!loadingInstance) {
        loadingInstance = Loading.service({
            lock: true,
            text,
            background: 'rgba(0, 0, 0, 0.3)',
            customClass: 'app-global-loading'
        })
    }
    return loadingInstance
}

export function hideLoading() {
    pendingCount = Math.max(0, pendingCount - 1)
    if (pendingCount === 0 && loadingInstance) {
        loadingInstance.close()
        loadingInstance = null
    }
}
