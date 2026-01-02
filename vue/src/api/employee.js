import request from './request' // 引入“电话总机”

// 1. 查询全部表格
// (对应后端 GET /employee/selectAll)
export function selectAllEmployee(params) {
    return request({
        url: '/employee/selectAll',
        method: 'get'
    })
}

// 2. 新增员工
// (对应后端 POST /employee/add)
export function addEmployee(data) {
    return request({
        url: '/employee/add',
        method: 'post',
        data: data // 'data' 会自动把对象转成 JSON 放在 Body 里
    })
}

// 3. 修改员工
// (对应后端 PUT /employee/update)
export function updateEmployee(data) {
    return request({
        url: '/employee/update',
        method: 'put',
        data: data
    })
}

// 4. 删除员工
// (对应后端 DELETE /employee/delete/{id})
export function deleteEmployee(id) {
    return request({
        url: `/employee/delete/${id}`, // 使用 ES6 模板字符串拼接 URL
        method: 'delete'
    })
}


// 5.分页查询
export function getEmployeePage(params) {
    return request({
        url: '/employee/page',
        method: 'get',
        params: params // 这里会把 { pageNum: 1, pageSize: 10 } 拼接到 URL 后
    })
}

// 6.批量删除
export function deleteEmployeeBatch(ids) {
    return request({
        url: '/employee/deleteBatch',
        method: 'delete',
        data: ids // 列表数据放在请求体中发送
    })
}

