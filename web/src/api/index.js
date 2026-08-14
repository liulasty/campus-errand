import http from '../utils/request'

// import aliyun from '../utils/ailiyun.js'

export const getData = (id) => {
    console.log("获取最新委托");
    return http.get('/tasks/newest/' + id)
}

export const adminActivation = (id) => {
    console.log("辅助激活用户");
    return http.put('/admin/users/' + id + '/activate')
}


export const deleteAccounts = (data) => {
    if (!Array.isArray(data)) {
        data = [data];
    }


    console.log(data.length === 1 ? "删除单个用户" : "批量删除用户", data);

    if (data.length === 0) {
        throw new Error("Empty input. Expected at least one userId.");
    }

    return http.delete('/admin/users', { data });
}


export const getTaskCategories = (id) => {
    console.log("获取委托的分类类别");
    return http.get('/categories/options')
}

export const addTaskDraft = (data) => {
    console.log("添加用户草稿")
    return http.post('/tasks/drafts', data)
}

export const getTaskDraftById = (id) => {
    console.log("用户获取委托草稿");
    return http.get('/tasks/drafts', { params: { userId: id } })
}

export const getDraftDetailsBasedOnCommissionId = (id) => {
    console.log("根据委托id获取委托草稿详情");
    return http.get('/tasks/' + id)
}

export const updateTaskDraft = (data) => {
    console.log("更新委托草稿");
    return http.put('/tasks/drafts', data)
}

export const deleteTaskDraft = (id) => {
    console.log("删除委托草稿");
    return http.delete('/tasks/drafts/' + id)
}

export const submitTaskDraft = (id) => {
    console.log("提交委托草稿");
    return http.put('/tasks/drafts/' + id + '/submit')
}

export const confirmTask = (id) => {
    console.log("获取需要发布的委托");
    return http.get('/tasks/' + id + '/confirm')
}

export const publishingDelegation = (data) => {
    console.log("发布委托");
    return http.put('/tasks/publisher/tasks/' + data.id + '/publish', data)
}


export const getReason = (id) => {
    console.log("获取审核不通过的原因");
    return http.get('/tasks/' + id + '/audit-reason')
}

export const getUserInfo = (id) => {
    // console.log("获取用户信息");
    return http.get('/authentications/' + id)
}

// 获取用户基础资料（UserProfileController：/user/profile/{id}，含 username/creditScore；区别于上面的 /authentications 实名信息接口）
export const fetchUserBasicInfo = (id) => {
    return http.get('/user/profile/' + id)
}

export const uploadImg = (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return http.post('/files/images', formData)
}

export const submitCertificationInformation = (data) => {
    console.log("提交认证信息");
    return http.post('/authentications', data)
}




export const confirmToPassTheReview = (id) => {
    console.log("确认通过审核");
    return http.put('/admin/authentications/' + id + '/approve')
}


export const refuseToPassReview = (id, reason) => {
    console.log("拒绝通过审核");
    return http.put('/admin/authentications/' + id + '/reject', null, { params: { reason } })
}

export const getUserList = (listSelectCondition) => {
    // console.log("查询参数", listSelectCondition);
    return http.get('/admin/users', {
        params: listSelectCondition,
    })
}
// 查询存储委托信息记录列表
export function listDelegateRecords(query) {
    console.log("查询存储委托信息记录列表:");
    return http.get('/admin/tasks', {
        params: query
    })
}


export function listDelegateUpdateRecords(query) {
    console.log("查询存储委托信息更新记录列表:");
    return http.get('/admin/tasks/updates', {
        params: query
    })
}

export function listTaskUpdateRecords(query) {
    console.log("查询任务履约动态列表（用户侧）:");
    return http.get('/tasks/updates', {
        params: query
    })
}

export const getDelegateUpdateType = () => {
    console.log("获取委托记录更新记录类型");
    return http.get('/admin/tasks/updates/types')
}

export function getDelegateByTaskID(TaskID) {
    return http.get('/admin/tasks/' + TaskID)
}

// 删除存储委托信息审核记录
export const delDelegateUpdateRecords = (RecordID) => {
    return http.delete('/admin/tasks/updates/' + RecordID)
}

export const addTaskUpdate = (data) => {
    console.log("添加任务进度更新");
    return http.post('/tasks/updates', data)
}

export const addTaskNodeUpdate = (data) => {
    console.log("添加任务履约节点打卡", data);
    return http.post('/tasks/updates/node', data)
}


export const deleteCertificationRecords = (id) => {
    // console.log("删除认证记录");
    return http.delete('/admin/authentications/' + id)
}

export const deleteAuthenticationInformation = (id) => {
    // console.log("删除认证信息");
    return http.delete('/admin/authentications/' + id)
}

// 
export const cancelUserInfoAuthentication = (id) => {
    // console.log("取消认证");
    return http.put('/authentications/' + id + '/cancel')
}

export const delDelegate = (id) => {
    // console.log("管理员删除委托信息");
    return http.delete('/admin/tasks/' + id)
}


export const FallbackDraft = (id) => {
    console.log("管理员回退草稿");
    return http.put('/admin/tasks/' + id + '/fallback-draft')
}


export const allowPublish = (id) => {
    console.log("管理员允许发布");
    return http.put('/admin/tasks/' + id + '/allow-publish')
}

export const notAllowed = (id) => {
    console.log("管理员拒绝发布");
    return http.put('/admin/tasks/' + id + '/reject-publish')
}


export const handleEnableAdmin = (id) => {
    console.log("管理员启用用户");
    return http.put('/admin/users/' + id + '/enable')
}

export const handleDisableAdmin = (id) => {
    console.log("管理员禁用用户");
    return http.put('/admin/users/' + id + '/disable')
};


export const getViewDelegateRecord = (id) => {
    console.log("获取委托记录");
    return http.get('/tasks/updates/' + id)
}

export const getSystemBulletinList = (query) => {
    // console.log("获取系统公告列表");
    return http.get('/announcements', {
        params: query
    })
}

export const deleteSystemBulletin = (id) => {
    console.log("删除系统公告");
    return http.delete('/admin/announcements/' + id)
}

export const getSystemBulletinById = (id) => {
    // console.log("获取系统公告详情");
    return http.get('/announcements/' + id)
}

export const updateSystemBulletin = (data) => {
    // console.log("修改系统公告", data);
    return http.put('/admin/announcements', data)
}

export const createSystemBulletin = (data) => {
    // console.log("新增系统公告", data);
    return http.post('/admin/announcements', data)
}


export const listNotifications = (query) => {
    console.log("获取通知列表");
    return http.get('/notifications', {
        params: query
    })
}


export const getNotificationsType = () => {
    console.log("获取通知类型");
    return http.get('/notifications/types')
}

export const addNotification = (data) => {
    console.log("添加通知", data);

    return http.post('/admin/notifications', data)
}

export const sendNotification = (data) => {
    console.log("发送通知", data);

    return http.post('/admin/notifications/send', data)
}

export const getNotificationById = (id) => {
    console.log("获取通知详情", id);
    return http.get('/notifications/' + id)
}

export const updateNotificationAdmin = (data) => {
    console.log("修改通知", data);
    return http.put('/admin/notifications', data)
}

export const delNotification = (id) => {
    console.log("删除通知", id);
    return http.delete('/admin/notifications/' + id)
}

export const listNotificationReadRecords = (data) => {
    console.log("查看消息读取记录");

    return http.get("/admin/notification-read-status", {
        params: data
    })
}

export const delNotificationReadRecord = (id) => {
    console.log("删除消息阅读记录", id);

    return http.delete("/admin/notification-read-status/" + id)
}

export const getDelegationTypeList = (query) => {
    console.log("获取委托类型列表");
    return http.get('/admin/categories', {
        params: query
    })
}

export const getDelegationTypeById = (id) => {
    console.log("获取委托类型详情", id);
    return http.get('/admin/categories/' + id)
}

export const updateDelegationTypeAdmin = (data) => {
    console.log("修改委托类型", data);
    return http.put('/admin/categories', data)
}

export const addDelegationTypeAdmin = (data) => {
    console.log("添加委托类型", data);
    return http.post('/admin/categories', data)
}

export const deleteDelegationType = (id) => {
    console.log("删除委托类型", id);
    return http.delete('/admin/categories/' + id)
}

export const enableDelegationType = (id) => {
    console.log("更改委托委托类型", id);
    return http.put('/admin/categories/' + id + '/enable')
}

export const withdrawReleaseByTaskIDAdmin = (id) => {
    console.log("管理员撤回发布", id);
    return http.put('/admin/tasks/' + id + '/withdraw-release')
}

export const uploadAvatar = (file) => {
    const formData = new FormData();
    formData.append('file', file);

    return http.post('/files/images/avatar', formData);
};

export const updateImg = (formData) => {
    return http.post('/files/images', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },// 设置请求头为multipart/form-data类型
    })
}

export const deleteImg = (deleteImg) => {
    return http.delete('/files/images', { data: deleteImg })
}

export const login = (userInfo) => {
    console.log("登录参数", userInfo)
    return http.post('/auth/login', userInfo)
}

export const logout = () => {
    console.log("退出");
    return http.delete('/auth/logout')
}


export const register = (userInfo) => {
    console.log("注册参数", userInfo)
    return http.post('/auth/register', userInfo)
}

export const exportExcel = () => {
    return http.get("/admin/reviews/export",{
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
    return http.put('/notifications/' + id + '/read')
}

// ===== 委托评价（大厅「赞/差」）=====
export const addReview = (data) => {
    return http.post('/reviews', data)
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
    return http.get('/admin/sensitive-words')
}

export const addSensitiveWord = (word) => {
    return http.post('/admin/sensitive-words', { word })
}

export const deleteSensitiveWord = (id) => {
    return http.delete('/admin/sensitive-words/' + id)
}

export const checkSensitiveText = (text) => {
    return http.post('/sensitive-words/check', { text })
}

// ===== Excel 导出 =====
export const exportTaskList = () => {
    return http.get('/admin/tasks/export', {
        responseType: 'blob'
    })
}

export const exportUserList = () => {
    return http.get('/admin/users/export', {
        responseType: 'blob'
    })
}

// export const loadImageAsBase64 =(url) =>{
//     return aliyun.get(url)
// }