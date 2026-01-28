import request from './request' // 引入“电话总机”

/**
 * 员工打卡接口 (对应后端 /api/attendance/punch)
 * @returns {Promise}
 */
export function punch() {
    return request({
        url: '/api/attendance/punch',
        method: 'post'
    })
}

/**
 * 获取最近打卡记录接口 (对应后端 /api/attendance/recent)
 */
export function getRecentRecords() {
    return request({
        url: '/api/attendance/recent',
        method: 'get'
    })
}