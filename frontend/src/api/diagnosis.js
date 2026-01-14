import request from './request'

//上传影像文件
export function uploadDiagnosisImage(file) {
    const formData = new FormData()
    formData.append('file', file) // key="file" 必须和后端 @RequestParam("file") 一致

    return request({
        url: '/api/diagnosis/upload',
        method: 'post',
        data: formData // 直接传 FormData，request.js 会自动处理请求头
    })
}


//AI诊断接口
export function getDiagnosisResult(taskId) {
    return request({
        url: '/api/diagnosis/result', // Python的FastAPI地址
        method: 'get',
        params: { taskId }, // 传task_id参数
        isOriginal: true
    })
}

