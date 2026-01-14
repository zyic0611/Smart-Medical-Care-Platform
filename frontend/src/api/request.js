import axios from 'axios'
import { ElMessage } from 'element-plus'

// 1. 创建 Axios 实例（保留你的原有配置）
const request = axios.create({
    baseURL: 'http://localhost:9090', // 你的后端地址
    timeout: 10000 // 超时时间
})

// 2. 请求拦截器（核心：加 FormData 判断，不强制改文件请求的 Content-Type）
request.interceptors.request.use(config => {
    // 🌟 关键修改：只给「非文件请求」设置 JSON Content-Type
    if (!(config.data instanceof FormData)) {
        // 普通 JSON 请求：保留你的原有逻辑
        config.headers['Content-Type'] = 'application/json;charset=utf-8';
    }
    // 🌟 文件请求（FormData）：不设置 Content-Type，让浏览器自动生成 multipart/form-data

    // 保留你的 token 逻辑（不动）
    const token = localStorage.getItem('token')
    if (token) {
        config.headers['token'] = token
    }

    return config
}, error => {
    return Promise.reject(error)
})

// 3. 响应拦截器（完全保留你的原有逻辑，一字不改）
request.interceptors.response.use(
    response => {
        let res = response.data

        if (res instanceof Blob || res instanceof ArrayBuffer) {
            return res
        }

        if (typeof res === 'string') {
            try {
                res = JSON.parse(res)
            } catch (e) {
                ElMessage.error('后端响应格式错误')
                return Promise.reject(new Error('后端响应格式错误'))
            }
        }

        // 如果请求配置里包含 isOriginal: true，则原样返回整个 res 对象
        if (response.config.isOriginal) {
            return res
        }



        if (res.code ==200) {
            return res.data
        } else {
            ElMessage.error(res.msg || '业务处理失败')
            return Promise.reject(new Error(res.msg || 'Error'))
        }
    },
    error => {
        if(error.response.status===404){
            ElMessage.error('未找到请求接口')
        }else if(error.response.status===500){
            ElMessage.error('系统异常，请查看后端控制台报错')
        } else{
            console.error(error.message)
        }
        return Promise.reject(error)
    }
)

export default request