import Vue from 'vue';
import { MessageBox, Message } from 'element-ui'; // 请替换为实际使用的Vue库导入方式
import { showLoading, hideLoading } from './loading';

Vue.prototype.$confirm = MessageBox.confirm;
Vue.prototype.$message = Message;

const $confirm = Vue.prototype.$confirm;
const $message = Vue.prototype.$message;

/**
 * 执行确认操作并发起请求
 *
 * 返回值：成功（code === 1）返回 true；取消/关闭/失败返回 false。
 * 调用方可据此在成功后关闭弹窗并刷新列表。
 *
 * @param {Function} requestFn - 请求函数，接受参数并返回Promise
 * @param {Object} requestParams - 请求所需参数
 * @param {string} title - 确认对话框标题
 * @param {string} successMessage - 成功消息提示
 * @param {string} warningMessage - 警告消息提示
 * @param {string} errorMessage - 错误消息提示
 * @param {string} cancelMessage - 取消消息提示
 * @returns {Promise<boolean>}
 */
export async function executeConfirmedRequest(
    requestFn,
    requestParams,
    message = '确认信息',
    title = '确认信息',
    successMessage = '操作成功',
    warningMessage = '操作警告',
    errorMessage = '操作失败，请稍后重试',
    cancelMessage = '操作已取消'
) {
    try {
        await $confirm(message, title, {
            distinguishCancelAndClose: true,
            confirmButtonText: '确认',
            cancelButtonText: '取消'
        });
    } catch (e) {
        $message({
            type: 'info',
            message: cancelMessage
        });
        return false;
    }

    showLoading();
    try {
        const response = await requestFn(requestParams);

        if (!response || !response.data) {
            throw new Error('Invalid server response');
        }

        if (response.data.code === 1) {
            $message({
                message: response.data.msg,
                type: 'success'
            });
            return true;
        } else {
            $message({
                message: response.data.msg,
                type: 'warning'
            });
            return false;
        }
    } catch (error) {
        console.error('处理请求时发生错误', error);

        $message({
            message: errorMessage,
            type: 'info'
        });
        return false;
    } finally {
        hideLoading();
    }
}
