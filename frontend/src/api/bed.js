import request from './request'



// 查询空闲床位
// (对应后端 GET /bed/list)
export function selectFreeBed(params) {
    return request({
        url: '/bed/list',
        method: 'get'
    })
}


// 分页查询
export function getBedPage(params) {
    return request({
        url: '/bed/page',
        method: 'get', params
    })
}


//新增
export function addBed(data) {
    return request({
        url: '/bed/add',
        method: 'post', data
    })
}


//更新
export function updateBed(data) {
    return request({
        url: '/bed/update',
        method: 'put', data
    })
}


//删除
export function deleteBed(id) {
    return request({
        url: `/bed/delete/${id}`,
        method: 'delete'
    })
}
