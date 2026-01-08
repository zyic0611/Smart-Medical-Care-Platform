import axios from 'axios'
import { ElMessage } from 'element-plus' // 引入 Element Plus 的弹窗组件

// 1. 创建 Axios 实例
const request = axios.create({
    // 关键：统一设置后端的接口基础地址
    // 你的 Java 后端跑在 9090 端口
    baseURL: 'http://localhost:9090',
    timeout: 10000 // 请求超时时间 (毫秒)
})

// 2. 请求拦截器
request.interceptors.request.use(config => {

    // 告诉后端：我给你发的是 JSON 格式的数据
    config.headers['Content-Type'] = 'application/json;charset=utf-8';

    // (这就是“带钥匙”的逻辑) ---
    // 从浏览器缓存里取出 Token
    const token = localStorage.getItem('token')

    // 如果有 Token，就把它贴在请求头里
    if (token) {
        // 这里的 'token' 要和你 Java 后端 request.getHeader("token") 里的名字一样
        config.headers['token'] = token
    }

    return config
}, error => {
    return Promise.reject(error)
})

// 3. 响应拦截器 (最重要！用来统一处理后端返回的 Result)
//也就是后端数据返回的时候
request.interceptors.response.use(
    /**
     * response.data 拿到的就是你后端 Result.java 包装后的对象 { code, msg, data }
     */
    response => {
        let res = response.data

        if (res instanceof Blob || res instanceof ArrayBuffer) {
            return res
        }


        // 防御性代码：处理后端可能返回字符串的情况
        if (typeof res === 'string') {
            try {
                res = JSON.parse(res)
            } catch (e) {
                // 如果解析失败，说明真的只是一个字符串，直接报错
                ElMessage.error('后端响应格式错误')
                return Promise.reject(new Error('后端响应格式错误'))
            }
        }


        // 核心逻辑：检查业务状态码
        if (res.code ==200) {
            // 成功，只返回 data
            return res.data
        } else {
            // 业务失败 (code 500, 401, 400 ...)，弹窗提示
            ElMessage.error(res.msg || '业务处理失败')
            return Promise.reject(new Error(res.msg || 'Error'))
        }
    },
    /**
     * 处理网络错误 (比如 404, 503, 后端崩了)
     */
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

// 导出这个配置好的实例
export default request