import request from '@/utils/request'

export function createKnowledge(data) {
    return request({
        url: '/knowledge',
        method: 'post',
        data
    })
}

export function getKnowledge(id) {
    return request({
        url: `/knowledge/${id}`,
        method: 'get'
    })
}

export function updateKnowledge(id, data) {
    return request({
        url: `/knowledge/${id}`,
        method: 'put',
        data
    })
}

export function deleteKnowledge(id) {
    return request({
        url: `/knowledge/${id}`,
        method: 'delete'
    })
}

export function getKnowledgeTree(spaceId) {
    return request({
        url: `/knowledge/space/${spaceId}/tree`,
        method: 'get'
    })
}

export function uploadKnowledgeFile(id, file) {
    const formData = new FormData()
    formData.append('file', file)
    return request({
        url: `/knowledge/${id}/upload`,
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

export function uploadFile(file) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('category', 'knowledge')
    return request({
        url: '/files/upload',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

export function getKnowledgeFileUrl(id) {
    return `/api/knowledge/${id}/file`
}
