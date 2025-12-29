import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
    baseURL: '/api',
    timeout: 600000 // Request timeout
})

// Request interceptor
service.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token
        }
        return config
    },
    error => {
        console.log(error)
        return Promise.reject(error)
    }
)

// Response interceptor
service.interceptors.response.use(
    response => {
        const res = response.data
        // Assuming backend returns standard response structure
        // If backend returns raw data, adjust accordingly.
        // Based on typical Spring Boot structure, it might be wrapped.
        // Let's assume direct return for now or check backend code.
        return res
    },
    error => {
        console.log('err' + error)
        if (error.response && error.response.status === 401) {
            ElMessage({
                message: '登录已过期，请重新登录',
                type: 'error',
                duration: 5 * 1000
            })
            localStorage.removeItem('token')
            localStorage.removeItem('user')
            router.push('/login')
        } else {
            ElMessage({
                message: error.message || '请求失败',
                type: 'error',
                duration: 5 * 1000
            })
        }
        return Promise.reject(error)
    }
)

export default service
