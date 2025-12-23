import request from '@/utils/request'

export function getSpaces() {
    return request({
        url: '/spaces',
        method: 'get'
    })
}

export function getSpace(id) {
    return request({
        url: `/spaces/${id}`,
        method: 'get'
    })
}

export function createSpace(data) {
    return request({
        url: '/spaces',
        method: 'post',
        data
    })
}

export function updateSpace(id, data) {
    return request({
        url: `/spaces/${id}`,
        method: 'put',
        params: data
    })
}

export function deleteSpace(id) {
    return request({
        url: `/spaces/${id}`,
        method: 'delete'
    })
}
