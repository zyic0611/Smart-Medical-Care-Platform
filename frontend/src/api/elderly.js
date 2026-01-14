import request from './request'

// 分页查询老人列表
export function getElderlyPage(params) {
    return request({
        url: '/elderly/page',
        method: 'get',
        params: params
    })
}

//分页查询有影像的老人
export function getElderlyWithImgPage(params) {
    return request({
        url: '/elderly/pagewithimages',
        method: 'get',
        params: params
    })
}

// 新增老人
// 对应后端: @PostMapping("/elderly/add")
// 参数 data 是一个对象 { name: '...', age: 80, nurseId: 1, bedId: 1 }
export function addElderly(data) {
    return request({
        url: '/elderly/add',
        method: 'post',
        data: data
    })
}

// 更新老人
// 对应后端: @Mapping("/elderly/update")
// 参数 data 是一个对象 { name: '...', age: 80, nurseId: 1, bedId: 1 }
export function updateElderly(data) {
    return request({
        url: '/elderly/update',
        method: 'put',
        data: data
    })
}

// 删除老人
// 对应后端: @Mapping("/elderly/delete/{id}")
//id是一个数字
export function deleteElderly(id) {
    return request({
        url: `/elderly/delete/${id}`,//注意这里用了反引号 `拼接 URL
        method: 'delete'
    })
}
