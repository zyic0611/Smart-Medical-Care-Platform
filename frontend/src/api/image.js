import request from './request' // 引入“电话总机”


//根据id查询影像信息
// (对应后端 api /imaging /selectByElderId)
export function selectByElderId(id) {
    return request({
        url: '/api/imaging/selectByElderId',
        method: 'get',
        params:{
            elderId:id
        }
    })
}


//下载影像
// (对应后端 api /imaging /download)
export function download(id) {
    return request({
        url: '/api/imaging/download',
        method: 'get',
        params:{
            id:id
        },
        responseType: 'blob' // 代表下载下来
    })
}


//新增影像
export function addimage(data) {
    return request({
        url: '/api/imaging/add',
        method: 'post',
        data:data
    })
}




