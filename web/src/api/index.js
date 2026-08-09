import http from '../utils/request'

// import aliyun from '../utils/ailiyun.js'

export const getData = (id) => {
    console.log("获取最新委托");
    return http.get('/task/getNewTask/' + id)
}

export const adminActivation = (id) => {
    console.log("辅助激活用户");
    return http.put('/user/adminActivation/' + id)
}


export const deleteAccounts = (data) => {
    if (!Array.isArray(data)) {
        data = [data];
    }


    console.log(data.length === 1 ? "删除单个用户" : "批量删除用户", data);

    if (data.length === 0) {
        throw new Error("Empty input. Expected at least one userId.");
    }

    return http.post('/user/deleteUser', data);
}


export const getTaskList = (data) => {
    console.log("获取委托列表");
    return http.post('/task/page', data)
}

export const getTaskCategories = (id) => {
    console.log("获取委托的分类类别");
    return http.get('/task/getTaskCategory')
}

export const addTaskDraft = (data) => {
    console.log("添加用户草稿")
    return http.post('/task/addTaskDraft', data)
}

export const getTaskDraftById = (id) => {
    console.log("用户获取委托草稿");
    return http.get('/task/getUserDelegateDraft/' + id)
}

export const getDraftDetailsBasedOnCommissionId = (id) => {
    console.log("根据委托id获取委托草稿详情");
    return http.get('/task/getTask/' + id)
}

export const updateTaskDraft = (data) => {
    console.log("更新委托草稿");
    return http.post('/task/updateTaskDraft', data)
}

export const deleteTaskDraft = (id) => {
    console.log("删除委托草稿");
    return http.delete('/task/deleteTaskDraft/' + id)
}

export const submitTaskDraft = (id) => {
    console.log("提交委托草稿");
    return http.put('/task/auditTask/' + id)
}

export const confirmTask = (id) => {
    console.log("获取需要发布的委托");
    return http.get('/task/confirmTask/' + id)
}

export const publishingDelegation = (data) => {
    console.log("发布委托");
    return http.put('/user/publisher/confirmTask/' + data.id, data)
}


export const getReason = (id) => {
    console.log("获取审核不通过的原因");
    return http.get('/task/getReason/' + id)
}

export const getUserInfo = (id) => {
    // console.log("获取用户信息");
    return http.get('/userInfo/' + id)
}

// 获取用户基础资料（UsersController：/user/getUserInfo/{id}，含 username/creditScore；区别于上面的 /userInfo 实名信息接口）
export const fetchUserBasicInfo = (id) => {
    return http.get('/user/getUserInfo/' + id)
}

export const uploadImg = (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return http.post('/img/upload', formData)
}

export const submitCertificationInformation = (data) => {
    console.log("提交认证信息");
    return http.post('/userInfo', data)
}




export const confirmToPassTheReview = (id) => {
    console.log("确认通过审核");
    return http.put('/userInfo/confirmToPassTheReview/' + id)
}


export const refuseToPassReview = (id, reason) => {
    console.log("拒绝通过审核");
    return http.put('/userInfo/refuseToPassReview/' + id, null, { params: { reason } })
}

export const getUserList = (listSelectCondition) => {
    // console.log("查询参数", listSelectCondition);
    return http.get('/user/page', {
        params: listSelectCondition,
    })
}
// 查询存储委托信息记录列表
export function listDelegateRecords(query) {
    console.log("查询存储委托信息记录列表:");
    return http.get('/admin/task/list', {
        params: query
    })
}


export function listDelegateUpdateRecords(query) {
    console.log("查询存储委托信息更新记录列表:");
    return http.get('/taskUpdate/list', {
        params: query
    })
}

export const getDelegateUpdateType = () => {
    console.log("获取委托记录更新记录类型");
    return http.get('/taskUpdate/type')
}

export function getDelegateByTaskID(TaskID) {
    return http.get('/admin/task/' + TaskID)
}

// 删除存储委托信息审核记录
export const delDelegateUpdateRecords = (RecordID) => {
    return http.delete('/taskUpdate/' + RecordID)
}

export const addTaskUpdate = (data) => {
    console.log("添加任务进度更新");
    return http.post('/taskUpdate/add', data)
}

export const addTaskNodeUpdate = (data) => {
    console.log("添加任务履约节点打卡", data);
    return http.post('/taskUpdate/node', data)
}


export const deleteCertificationRecords = (id) => {
    // console.log("删除认证记录");
    return http.delete('/userInfo/' + id)
}

export const deleteAuthenticationInformation = (id) => {
    // console.log("删除认证信息");
    return http.delete('/userInfo/' + id)
}

// 
export const cancelUserInfoAuthentication = (id) => {
    // console.log("取消认证");
    return http.put('/userInfo/cancelUserInfoAuthentication/' + id)
}

export const delDelegate = (id) => {
    // console.log("管理员删除委托信息");
    return http.delete('/admin/task/' + id)
}


export const FallbackDraft = (id) => {
    console.log("管理员回退草稿");
    return http.put('/admin/task/getFallbackDraft/' + id)
}


export const allowPublish = (id) => {
    console.log("管理员允许发布");
    return http.put('/admin/task/allowPublish/' + id)
}

export const notAllowed = (id) => {
    console.log("管理员拒绝发布");
    return http.put('/admin/task/notAllowed/' + id)
}


export const handleEnableAdmin = (id) => {
    console.log("管理员启用用户");
    return http.put('/user/handleEnableByAdmin/' + id)
}

export const handleDisableAdmin = (id) => {
    console.log("管理员禁用用户");
    return http.put('/user/handleDisableByAdmin/' + id)
};


export const getViewDelegateRecord = (id) => {
    console.log("获取委托记录");
    return http.get('/taskUpdate/getTask/' + id)
}

export const getSystemBulletinList = (query) => {
    // console.log("获取系统公告列表");
    return http.get('/system-announcements/list', {
        params: query
    })
}

export const deleteSystemBulletin = (id) => {
    console.log("删除系统公告");
    return http.delete('/system-announcements/' + id)
}

export const getSystemBulletinById = (id) => {
    // console.log("获取系统公告详情");
    return http.get('/system-announcements/' + id)
}

export const updateSystemBulletin = (data) => {
    // console.log("修改系统公告", data);
    return http.put('/system-announcements', data)
}

export const createSystemBulletin = (data) => {
    // console.log("新增系统公告", data);
    return http.post('/system-announcements', data)
}


export const listNotifications = (query) => {
    console.log("获取通知列表");
    return http.get('/notifications/list', {
        params: query
    })
}


export const getNotificationsType = () => {
    console.log("获取通知类型");
    return http.get('/notifications/type')
}

export const addNotification = (data) => {
    console.log("添加通知", data);

    return http.post('/notifications', data)
}

export const sendNotification = (data) => {
    console.log("发送通知", data);

    return http.post('/notifications/send', data)
}

export const getNotificationById = (id) => {
    console.log("获取通知详情", id);
    return http.get('/notifications/' + id)
}

export const updateNotificationAdmin = (data) => {
    console.log("修改通知", data);
    return http.put('/notifications', data)
}

export const delNotification = (id) => {
    console.log("删除通知", id);
    return http.delete('/notifications/' + id)
}

export const listNotificationReadRecords = (data) => {
    console.log("查看消息读取记录");

    return http.get("/notificationReadStatus/list", {
        params: data
    })
}

export const getDelegationTypeList = (query) => {
    console.log("获取委托类型列表");
    return http.get('/delegation_categories/list', {
        params: query
    })
}

export const getDelegationTypeById = (id) => {
    console.log("获取委托类型详情", id);
    return http.get('/delegation_categories/' + id)
}

export const updateDelegationTypeAdmin = (data) => {
    console.log("修改委托类型", data);
    return http.put('/delegation_categories', data)
}

export const addDelegationTypeAdmin = (data) => {
    console.log("添加委托类型", data);
    return http.post('/delegation_categories', data)
}

export const deleteDelegationType = (id) => {
    console.log("删除委托类型", id);
    return http.delete('/delegation_categories/' + id)
}

export const enableDelegationType = (id) => {
    console.log("更改委托委托类型", id);
    return http.put('/delegation_categories/enable/' + id)
}

export const withdrawReleaseByTaskIDAdmin = (id) => {
    console.log("管理员撤回发布", id);
    return http.put('/admin/task/withdrawReleaseByTaskID/' + id)
}

export const uploadAvatar = (file) => {
    const formData = new FormData();
    formData.append('file', file);

    return http.post('/img/uploadAvatar', formData);
};

export const updateImg = (formData) => {
    return http.post('/img/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },// 设置请求头为multipart/form-data类型
    })
}

export const deleteImg = (deleteImg) => {
    return http.delete('/img/delete', { data: deleteImg })
}

export const login = (userInfo) => {
    console.log("登录参数", userInfo)
    return http.post('/user/login', userInfo)
}

export const logout = () => {
    console.log("退出");
    return http.delete('user/logout')
}


export const register = (userInfo) => {
    console.log("注册参数", userInfo)
    return http.post('/user/register', userInfo)
}

export const exportExcel = () => {
    return http.get("/reviews/exportExcel",{
        responseType: 'blob'
    })
}

// ===== 消息中心 =====
export const getMyNotifications = (query) => {
    return http.get('/notifications/my', {
        params: query
    })
}

export const markNotificationRead = (id) => {
    return http.put('/notifications/read/' + id)
}

// ===== 信用档案 =====
export const getCreditProfile = () => {
    return http.get('/credit')
}

// ===== 数据驾驶舱 =====
export const getDashboardStats = () => {
    return http.get('/stats')
}

// ===== 敏感词配置 =====
export const listSensitiveWords = () => {
    return http.get('/sensitive/words')
}

export const addSensitiveWord = (word) => {
    return http.post('/sensitive/words', { word })
}

export const deleteSensitiveWord = (id) => {
    return http.delete('/sensitive/words/' + id)
}

export const checkSensitiveText = (text) => {
    return http.post('/sensitive/check', { text })
}

// ===== Excel 导出 =====
export const exportTaskList = () => {
    return http.get('/admin/task/exportExcel', {
        responseType: 'blob'
    })
}

export const exportUserList = () => {
    return http.get('/user/exportExcel', {
        responseType: 'blob'
    })
}

// export const loadImageAsBase64 =(url) =>{
//     return aliyun.get(url)
// }