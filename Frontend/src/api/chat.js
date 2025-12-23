import request from '@/utils/request'

export function chat(data) {
    return request({
        url: '/knowledge/chat',
        method: 'post',
        data
    })
}

export async function chatStream(data, onMessage, onError, onComplete) {
    const token = localStorage.getItem('token')
    try {
        const response = await fetch('/api/knowledge/chat/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': token ? `Bearer ${token}` : ''
            },
            body: JSON.stringify(data)
        })

        if (!response.ok) {
            throw new Error(response.statusText)
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
            const { done, value } = await reader.read()
            if (done) {
                if (onComplete) onComplete()
                break
            }
            buffer += decoder.decode(value, { stream: true })

            // Parse SSE events separated by a blank line (\n\n)
            let eventEnd = buffer.indexOf('\n\n')
            while (eventEnd !== -1) {
                const rawEvent = buffer.slice(0, eventEnd)
                buffer = buffer.slice(eventEnd + 2)

                const dataLines = []
                for (const line of rawEvent.split('\n')) {
                    if (line.startsWith('data:')) {
                        dataLines.push(line.slice(5).trimStart())
                    }
                }
                if (dataLines.length > 0) {
                    if (onMessage) onMessage(dataLines.join('\n'))
                }

                eventEnd = buffer.indexOf('\n\n')
            }
        }
    } catch (error) {
        if (onError) onError(error)
    }
}
