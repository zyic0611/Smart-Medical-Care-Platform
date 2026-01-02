import request from './request' // 引入“电话总机”
// ==========================================
//  异步多线程下载专用接口
// ==========================================

// 1. 开启下载任务
// 对应后端: @GetMapping("/start/{id}")
export function startAsyncDownload(id) {
    return request({
        // 注意：这里用了反引号 ` 拼接 URL，因为后端是 @PathVariable
        url: `/api/async-download/start/${id}`,
        method: 'get'
    })
}

// 2. 轮询查询任务状态
// 对应后端: @GetMapping("/status/{taskId}")
export function getDownloadStatus(taskId) {
    return request({
        url: `/api/async-download/status/${taskId}`,
        method: 'get'
    })
}

// 3. 任务完成后，获取最终文件流
// 对应后端: @GetMapping("/fetch/{taskId}")
export function fetchDownloadedFile(taskId) {
    return request({
        url: `/api/async-download/fetch/${taskId}`,
        method: 'get',
        responseType: 'blob' // 【关键】必须加这个，因为返回的是二进制文件流
    })
}
