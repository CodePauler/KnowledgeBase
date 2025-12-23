import request from '@/utils/request'

export function login(data) {
    return request({
        url: '/auth/login',
        method: 'post',
        params: data // Using params because controller uses @RequestParam
    })
}

export function register(data) {
    return request({
        url: '/auth/register',
        method: 'post',
        params: data
    })
}
