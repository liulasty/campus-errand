
import http from '../utils/request'

export const listViewOnGoingList = (data) => {
    return http.get('/tasks/hall', {
        params: data,
    })
}

export const getTaskCategoriesUser = () => {
    return http.get('/tasks/hall/categories')
}


export const queryTheEntrustmentDetailsByEntrustmentNumber = (id) => {
    return http.get('/tasks/publisher/tasks/' + id)
}

export const getTaskAndPublishUserInfoByTaskId = (id) => {
    return http.get('/tasks/hall/' + id)
}

export const getAcceptedTaskBrief = (taskId) => {
    return http.get('/tasks/accepts/' + taskId + '/task')
}

export const getPublisherTaskBrief = (taskId) => {
    return http.get('/tasks/publisher/' + taskId + '/task')
}

export const acceptCommission = (data) => {
    console.log("接收委托留言");
    return http.post('/tasks/accepts', data)
}


export const publishDelegationList = (data) => {
    return http.get('/tasks/publisher/tasks', {
        params: data,
    })
}
export const getTaskAcceptById = (id) => {
    return http.get('/tasks/accepts/' + id)
}


export const acceptDelegationList = (data) => {
    return http.get('/tasks/accepts', {
        params: data,
    })
}

export const cancelAcceptorByAcceptor = (id) => {
    return http.put('/tasks/accepts/' + id + '/cancel')
}


export const confirmTheRecipient = (id) => {
    return http.put('/tasks/publisher/accepts/' + id + '/confirm')
}

export const getPersonalNoticeList = (data) => {
    return http.get('/notifications/by-type/' + data)
}


export const getNoticeById = (id) => {
    return http.get('/notifications/info/' + id)
}

export const cancelPublishUser = (id) => {
    return http.put('/tasks/publisher/tasks/' + id + '/cancel-publish')
}

export const updateDelegationCompleted = (data) => {
    return http.put('/tasks/publisher/tasks/' + data.taskId + '/completed', data)
}

export const deleteTaskByPublisher = (id) => {
    return http.delete('/tasks/publisher/tasks/' + id)
}

export const publisherFallbackDraft = (id) => {
    return http.put('/tasks/publisher/tasks/' + id + '/fallback-draft')
}