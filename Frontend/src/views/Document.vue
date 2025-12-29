<template>
    <div class="document-container" v-loading="loading">
        <div class="doc-header">
            <div class="title-area">
                <h1 v-if="!isEditing">{{ knowledge.title }}</h1>
                <el-input v-else v-model="editForm.title" size="large" />
            </div>
            <div class="actions">
                <template v-if="knowledge.type === 'MANUAL'">
                    <template v-if="!isEditing">
                        <el-button @click="startEdit">编辑</el-button>
                        <el-button @click="downloadFile">下载</el-button>
                    </template>
                    <template v-else>
                        <el-button @click="cancelEdit">取消</el-button>
                        <el-button type="primary" @click="saveEdit">保存</el-button>
                        <el-button type="success" @click="toggleAiAssistant">
                            <el-icon style="margin-right: 4px;">
                                <EditPen />
                            </el-icon>
                            AI帮写
                        </el-button>
                    </template>
                </template>
                <template v-else-if="knowledge.type === 'DOC'">
                    <el-button type="success" @click="openDocEdit">更新</el-button>
                    <el-button @click="downloadFile">下载原文件</el-button>
                </template>
            </div>
        </div>

        <!-- DOC 更新对话框 -->
        <el-dialog v-model="docEditVisible" title="更新文档" width="520px">
            <el-form label-position="top">
                <el-form-item label="标题">
                    <el-input v-model="docForm.title" />
                </el-form-item>
                <el-form-item label="选择新文件（可选）">
                    <el-upload class="upload-doc" action="#" :auto-upload="false" :show-file-list="false"
                        :on-change="onSelectDocFile">
                        <el-button>选择文件</el-button>
                    </el-upload>
                    <span v-if="docForm.fileName" style="margin-left: 12px; color:#8f959e;">{{ docForm.fileName
                        }}</span>
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="docEditVisible = false">取消</el-button>
                    <el-button type="primary" @click="submitDocUpdate">保存</el-button>
                </span>
            </template>
        </el-dialog>

        <div class="doc-content">
            <!-- MANUAL 编辑/预览 -->
            <template v-if="knowledge.type === 'MANUAL'">
                <div v-if="!isEditing" class="markdown-body" v-html="renderedContent"></div>
                <div v-else>
                    <!-- AI帮写输入区域 -->
                    <div v-if="showAiInput" class="ai-input-panel">
                        <div class="ai-input-header">
                            <span class="ai-title">
                                <el-icon style="margin-right: 4px;">
                                    <EditPen />
                                </el-icon>
                                AI帮写
                            </span>
                            <el-button text @click="closeAiInput">
                                <el-icon>
                                    <Close />
                                </el-icon>
                            </el-button>
                        </div>
                        <el-input v-model="aiPrompt" type="textarea" :rows="3"
                            placeholder="描述你需要AI帮助生成的内容，例如：“写一段关于Vue3响应式原理的介绍”" @keydown.ctrl.enter="generateWithAi" />
                        <div class="ai-input-actions">
                            <el-button size="small" @click="closeAiInput">取消</el-button>
                            <el-button size="small" type="primary" @click="generateWithAi" :loading="aiGenerating">
                                {{ aiGenerating ? '生成中...' : '生成 (Ctrl+Enter)' }}
                            </el-button>
                        </div>
                    </div>

                    <!-- AI生成控制面板 -->
                    <div v-if="aiContentGenerated" class="ai-control-panel">
                        <div class="ai-control-info">
                            <el-icon style="color: #67c23a; margin-right: 4px;"><Select /></el-icon>
                            <span>AI已生成内容，请选择是否接受</span>
                        </div>
                        <div class="ai-control-actions">
                            <el-button size="small" @click="rejectAiContent">
                                <el-icon style="margin-right: 4px;">
                                    <Close />
                                </el-icon>
                                撤销
                            </el-button>
                            <el-button size="small" type="success" @click="acceptAiContent">
                                <el-icon style="margin-right: 4px;">
                                    <Check />
                                </el-icon>
                                接受
                            </el-button>
                        </div>
                    </div>

                    <div id="vditor-container" class="editor-area"></div>
                </div>
            </template>

            <template v-else-if="knowledge.type === 'DOC'">
                <!-- PDF 文件预览 -->
                <template v-if="isPdf">
                    <div class="pdf-toolbar" v-if="!pdfLoading && !pdfError && pdfSrc">
                        <el-button size="small" @click="zoomOut">-</el-button>
                        <span class="zoom">{{ (scale * 100).toFixed(0) }}%</span>
                        <el-button size="small" @click="zoomIn">+</el-button>
                        <el-divider direction="vertical" />
                        <el-button size="small" @click="prevPage">上一页</el-button>
                        <span class="page">{{ page }} / {{ numPages }}</span>
                        <el-button size="small" @click="nextPage">下一页</el-button>
                    </div>
                    <div class="pdf-container">
                        <div v-if="pdfLoading" class="pdf-status">正在加载 PDF...</div>
                        <div v-else-if="pdfError" class="pdf-status">{{ pdfError }}</div>
                        <vue-pdf-embed v-else class="pdf-view" :source="pdfSrc" :page="page" @loaded="onPdfLoaded"
                            :style="{ transform: `scale(${scale})` }" />
                    </div>
                </template>
                <!-- DOCX 预览 -->
                <template v-else-if="isDocx">
                    <div v-show="docxLoading" class="docx-status">正在加载 Word 文档...</div>
                    <div v-show="docxError" class="docx-status">{{ docxError }}</div>
                    <div v-show="!docxLoading && !docxError" id="docx-preview-container" class="docx-container"></div>
                </template>
                <!-- 不支持的文件类型提示 -->
                <template v-else>
                    <div class="doc-info-panel">
                        <el-card>
                            <template #header>
                                <div style="display: flex; justify-content: space-between; align-items: center;">
                                    <span>📄 {{ knowledge.title }}</span>
                                    <el-button type="primary" size="small" @click="downloadFile">下载文件</el-button>
                                </div>
                            </template>
                            <el-alert title="不支持的文件类型" type="info" description="此文件类型暂不支持在线预览。请点击下载按钮获取原文件进行查看。"
                                :closable="false" style="margin-bottom: 16px" />
                            <div v-if="fileExtension" style="margin-bottom: 16px; color: #606266;">
                                <strong>文件类型：</strong>
                                <el-tag type="info">{{ fileExtension }}</el-tag>
                            </div>
                        </el-card>
                    </div>
                </template>
            </template>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, watch, computed, inject } from 'vue'
import { useRoute } from 'vue-router'
import { getKnowledge, updateKnowledge, uploadFile, getKnowledgeFileBlob } from '@/api/knowledge'
import { ElMessage } from 'element-plus'
import { EditPen, Close, Select, Check } from '@element-plus/icons-vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import VuePdfEmbed from 'vue-pdf-embed'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { renderAsync } from 'docx-preview'

const route = useRoute()
const knowledge = ref({})
const loading = ref(false)
const isEditing = ref(false)
const refreshTree = inject('refreshTree', null) // 注入刷新树的方法
const editForm = ref({
    title: '',
    content: ''
})
let vditorInstance = null

// DOC 更新对话框
const docEditVisible = ref(false)
const docForm = ref({
    title: '',
    file: null,
    fileName: ''
})

// AI帮写相关
const showAiInput = ref(false)
const aiPrompt = ref('')
const aiGenerating = ref(false)
const aiContentGenerated = ref(false)
const contentBeforeAi = ref('')

const fetchKnowledge = async () => {
    const id = route.params.docId
    if (!id) return
    loading.value = true
    try {
        const res = await getKnowledge(id)
        if (res.code === 200) {
            knowledge.value = res.data
            if (knowledge.value.type !== 'MANUAL') {
                isEditing.value = false
                await loadDocPreview()
            }
        }
    } catch (error) {
        console.error(error)
    } finally {
        loading.value = false
    }
}

const renderedContent = computed(() => {
    if (!knowledge.value.content) return '<p>点击“编辑”按钮开始创作</p>'
    return marked(knowledge.value.content, {
        highlight: (code, lang) => {
            const language = hljs.getLanguage(lang) ? lang : 'plaintext'
            return hljs.highlight(code, { language }).value
        }
    })
})

const startEdit = () => {
    if (knowledge.value.type !== 'MANUAL') {
        return
    }
    editForm.value = {
        title: knowledge.value.title,
        content: knowledge.value.content || ''
    }
    isEditing.value = true
    // 初始化 Vditor
    nextTick(() => {
        initVditor()
    })
}

const initVditor = () => {
    if (vditorInstance) {
        vditorInstance.destroy()
    }
    vditorInstance = new Vditor('vditor-container', {
        height: 600,
        toolbarConfig: {
            pin: true,
        },
        mode: 'ir',
        preview: {
            mode: 'both',
            url: '',
            parse: (element) => {
                if (element.parentClassName === 'vditor-preview') {
                    Vditor.preview(element, {
                        transform(html) {
                            return html
                        },
                    }, undefined)
                }
            },
        },
        upload: {
            url: '',
            max: 0,
        },
        input: (value) => {
            editForm.value.content = value
        },
        after: () => {
            vditorInstance.setValue(editForm.value.content)
        },
    })
}

const nextTick = (fn) => {
    Promise.resolve().then(fn)
}

const cancelEdit = () => {
    if (vditorInstance) {
        vditorInstance.destroy()
        vditorInstance = null
    }
    isEditing.value = false
}

const saveEdit = async () => {
    try {
        // 确保最新的内容被保存
        if (vditorInstance) {
            editForm.value.content = vditorInstance.getValue()
        }
        const res = await updateKnowledge(knowledge.value.id, {
            title: editForm.value.title,
            content: editForm.value.content
        })
        if (res.code === 200) {
            ElMessage.success('保存成功')
            knowledge.value = res.data
            isEditing.value = false
            if (vditorInstance) {
                vditorInstance.destroy()
                vditorInstance = null
            }
            // 刷新左侧知识树
            if (refreshTree) {
                refreshTree()
            }
        } else {
            ElMessage.error(res.msg || '保存失败')
        }
    } catch (error) {
        console.error(error)
    }
}

const downloadFile = async () => {
    if (knowledge.value.type === 'DOC') {
        // 使用fetch带token下载,而不是window.open
        try {
            const token = localStorage.getItem('token')
            const res = await fetch(`/api/knowledge/${knowledge.value.id}/file`, {
                headers: { 'Authorization': token ? `Bearer ${token}` : '' }
            })
            if (!res.ok) {
                ElMessage.error('下载失败')
                return
            }
            const blob = await res.blob()
            const url = URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            a.download = knowledge.value.title || 'download'
            document.body.appendChild(a)
            a.click()
            URL.revokeObjectURL(url)
            document.body.removeChild(a)
        } catch (error) {
            console.error(error)
            ElMessage.error('下载失败')
        }
        return
    }
    // MANUAL: 导出为 .md 文件
    const content = knowledge.value.content || ''
    const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    const base = knowledge.value.title || 'document'
    const filename = base.toLowerCase().endsWith('.md') ? base : `${base}.md`
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    URL.revokeObjectURL(url)
    document.body.removeChild(a)
}

// 打开 DOC 更新对话框
const openDocEdit = () => {
    docForm.value = {
        title: knowledge.value.title,
        file: null,
        fileName: ''
    }
    docEditVisible.value = true
}

// 自定义选择文件（不自动上传）
const onSelectDocFile = (file, fileList) => {
    // on-change 事件返回的file对象包含raw属性，raw才是真正的File对象
    docForm.value.file = file.raw
    docForm.value.fileName = file.name
}

// 提交 DOC 更新：可只改标题，也可同时更换文件
const submitDocUpdate = async () => {
    // 添加loading状态
    loading.value = true
    try {
        let payload = { title: docForm.value.title }

        // 如果选择了新文件，先上传
        if (docForm.value.file) {
            ElMessage.info('正在上传文件...')
            const uploadRes = await uploadFile(docForm.value.file)
            if (uploadRes.code !== 200) {
                ElMessage.error(uploadRes.msg || '文件上传失败')
                return
            }
            payload.blobKey = uploadRes.data.blobKey
            ElMessage.success('文件上传成功')
        }

        // 更新知识
        ElMessage.info('正在更新知识...')
        const res = await updateKnowledge(knowledge.value.id, payload)
        if (res.code === 200) {
            ElMessage.success('更新成功')
            docEditVisible.value = false
            // 重新加载知识内容
            await fetchKnowledge()
            // 刷新左侧知识树
            if (refreshTree) {
                refreshTree()
            }
        } else {
            ElMessage.error(res.msg || '更新失败')
        }
    } catch (e) {
        console.error(e)
        ElMessage.error('更新失败')
    } finally {
        loading.value = false
    }
}

// AI帮写功能
const toggleAiAssistant = () => {
    showAiInput.value = !showAiInput.value
    if (showAiInput.value) {
        aiPrompt.value = ''
    }
}

const closeAiInput = () => {
    showAiInput.value = false
    aiPrompt.value = ''
}

const generateWithAi = async () => {
    if (!aiPrompt.value.trim() || aiGenerating.value) return
    if (!vditorInstance) {
        ElMessage.error('编辑器未初始化')
        return
    }

    aiGenerating.value = true

    // 保存生成前的内容
    contentBeforeAi.value = vditorInstance.getValue()

    try {
        // 构造prompt，包含当前markdown内容作为上下文
        const currentContent = contentBeforeAi.value
        const fullPrompt = currentContent
            ? `你是一个Markdown写作助手。当前Markdown文档内容如下：

${currentContent}

---

用户需求：${aiPrompt.value}

请直接输出符合用户需求的Markdown内容，不要添加任何说明文字，不要使用代码块包裹。如果用户要求补充或扩写，请生成可以直接追加的内容；如果用户要求修改，请生成完整的修改后内容。直接开始输出Markdown文本。`
            : `你是一个Markdown写作助手。

用户需求：${aiPrompt.value}

请直接输出符合用户需求的Markdown内容，不要添加任何说明文字，不要使用代码块包裹。直接开始输出Markdown文本。`

        const response = await fetch('/api/knowledge/chat/simple-stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: JSON.stringify({
                question: fullPrompt
            })
        })

        if (!response.ok) {
            throw new Error('请求失败')
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let generatedContent = ''

        console.log('开始读取AI响应流...')

        while (true) {
            const { done, value } = await reader.read()
            if (done) {
                console.log('流读取完成')
                break
            }

            buffer += decoder.decode(value, { stream: true })
            console.log('Buffer:', buffer)

            let eventEnd = buffer.indexOf('\n\n')
            while (eventEnd !== -1) {
                const rawEvent = buffer.slice(0, eventEnd)
                buffer = buffer.slice(eventEnd + 2)

                console.log('Raw event:', rawEvent)

                const dataLines = []
                for (const line of rawEvent.split('\n')) {
                    if (line.startsWith('data:')) {
                        const content = line.substring(5).trim() // 使用 substring 而不是 slice
                        dataLines.push(content)
                    }
                }

                if (dataLines.length > 0) {
                    let dataStr = dataLines.join('')

                    // 兜底：如果还有 'data:' 前缀，再次去除
                    if (dataStr.startsWith('data:')) {
                        dataStr = dataStr.substring(5).trim()
                    }

                    console.log('Data string:', dataStr)

                    if (dataStr === '[DONE]') {
                        console.log('收到DONE信号')
                        break
                    }
                    try {
                        const data = JSON.parse(dataStr)
                        console.log('Parsed data:', data)
                        if (data.content) {
                            generatedContent += data.content
                            console.log('Generated content length:', generatedContent.length)
                            // 实时更新编辑器内容
                            vditorInstance.setValue(currentContent + '\n\n' + generatedContent)
                        }
                    } catch (e) {
                        console.error('JSON解析错误:', e, 'Data:', dataStr)
                    }
                }

                eventEnd = buffer.indexOf('\n\n')
            }
        }

        console.log('最终生成内容长度:', generatedContent.length)
        console.log('生成内容前100字符:', generatedContent.substring(0, 100))

        if (generatedContent) {
            aiContentGenerated.value = true
            showAiInput.value = false
            ElMessage.success('AI内容已生成，请选择是否接受')
        } else {
            console.warn('generatedContent为空')
            ElMessage.warning('AI未生成任何内容')
            vditorInstance.setValue(contentBeforeAi.value)
        }

    } catch (error) {
        console.error('AI生成失败:', error)
        ElMessage.error('AI生成失败，请重试')
        // 恢复原内容
        if (vditorInstance) {
            vditorInstance.setValue(contentBeforeAi.value)
        }
    } finally {
        aiGenerating.value = false
    }
}

const acceptAiContent = () => {
    // 接受AI生成的内容
    aiContentGenerated.value = false
    contentBeforeAi.value = ''
    aiPrompt.value = ''
    ElMessage.success('已接受AI生成的内容')
}

const rejectAiContent = () => {
    // 撤销AI生成的内容，恢复原来的内容
    if (vditorInstance && contentBeforeAi.value !== undefined) {
        vditorInstance.setValue(contentBeforeAi.value)
    }
    aiContentGenerated.value = false
    contentBeforeAi.value = ''
    aiPrompt.value = ''
    ElMessage.info('已撤销AI生成的内容')
}

watch(() => route.params.docId, () => {
    fetchKnowledge()
    isEditing.value = false
})

onMounted(() => {
    fetchKnowledge()
})

// ===== PDF 预览逻辑 =====
const pdfSrc = ref(null)
const pdfLoading = ref(false)
const pdfError = ref('')
const scale = ref(1)
const page = ref(1)
const numPages = ref(0)
const isPdf = ref(false)
const isDocx = ref(false)
const docxLoading = ref(false)
const docxError = ref('')
const fileExtension = ref('')

const loadDocPreview = async () => {
    isPdf.value = false
    isDocx.value = false
    pdfError.value = ''
    docxError.value = ''
    pdfSrc.value = null
    pdfLoading.value = true
    docxLoading.value = true

    try {
        const blob = await getKnowledgeFileBlob(knowledge.value.id)
        if (!blob || !(blob instanceof Blob)) {
            pdfError.value = '预览失败：未获取到文件数据'
            docxError.value = '预览失败：未获取到文件数据'
            return
        }

        // 检测文件类型
        const title = knowledge.value.title || ''
        const mimeType = blob.type || ''

        if (mimeType.includes('pdf') || title.toLowerCase().endsWith('.pdf')) {
            // PDF 文件
            isPdf.value = true
            pdfSrc.value = URL.createObjectURL(blob)
            docxLoading.value = false
        } else if (mimeType.includes('wordprocessingml') || title.toLowerCase().endsWith('.docx')) {
            // DOCX 文件
            isDocx.value = true
            pdfLoading.value = false
            await nextTick()
            const container = document.getElementById('docx-preview-container')
            if (!container) {
                docxError.value = '预览容器未找到'
                return
            }
            container.innerHTML = ''
            await renderAsync(blob, container, null, {
                className: 'docx-wrapper',
                inWrapper: true,
                ignoreWidth: false,
                ignoreHeight: false,
                ignoreFonts: false,
                breakPages: true,
                ignoreLastRenderedPageBreak: true,
                experimental: true,
                trimXmlDeclaration: true,
                useBase64URL: true,
                renderHeaders: true,
                renderFooters: true,
                renderFootnotes: true,
                renderEndnotes: true
            })
        } else {
            // 不支持的文件类型
            isPdf.value = false
            isDocx.value = false
            pdfLoading.value = false
            docxLoading.value = false
            const match = title.match(/\.([a-zA-Z0-9]+)$/)
            if (match) {
                fileExtension.value = match[1].toUpperCase()
            }
        }
    } catch (e) {
        console.error(e)
        if (isPdf.value) {
            pdfError.value = '加载 PDF 文件失败'
        } else if (isDocx.value) {
            docxError.value = '加载 Word 文档失败: ' + (e.message || '未知错误')
        }
    } finally {
        pdfLoading.value = false
        docxLoading.value = false
    }
}

const onPdfLoaded = (info) => {
    // vue-pdf-embed emits loaded with { numPages }
    if (info && info.numPages) {
        numPages.value = info.numPages
    }
}

const zoomIn = () => { scale.value = Math.min(scale.value + 0.1, 3) }
const zoomOut = () => { scale.value = Math.max(scale.value - 0.1, 0.5) }
const prevPage = () => { page.value = Math.max(page.value - 1, 1) }
const nextPage = () => { page.value = Math.min(page.value + 1, numPages.value || page.value + 1) }
</script>

<style scoped lang="scss">
.document-container {
    padding: 20px 40px;
    height: 100%;
    overflow-y: auto;
    box-sizing: border-box;
}

.doc-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30px;
    border-bottom: 1px solid #eee;
    padding-bottom: 20px;

    h1 {
        margin: 0;
        font-size: 28px;
        color: #1f2329;
    }
}

.doc-content {
    font-size: 16px;
    line-height: 1.6;
    color: #1f2329;
}

.editor-area {
    margin-top: 20px;
    border-radius: 6px;
    overflow: hidden;
}

.markdown-body {

    /* Basic markdown styles */
    :deep(h1),
    :deep(h2),
    :deep(h3) {
        margin-top: 24px;
        margin-bottom: 16px;
        font-weight: 600;
        line-height: 1.25;
    }

    :deep(p) {
        margin-bottom: 16px;
    }

    :deep(pre) {
        background-color: #f6f8fa;
        padding: 16px;
        border-radius: 6px;
        overflow: auto;
    }

    :deep(code) {
        font-family: ui-monospace, SFMono-Regular, SF Mono, Menlo, Consolas, Liberation Mono, monospace;
    }
}

.unsupported {
    color: #8f959e;
    padding: 12px;
}

.pdf-container {
    min-height: 60vh;
    background: #f6f8fa;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 8px;
    overflow: auto;
    /* 防止放大时内容溢出，启用容器滚动 */
}

.pdf-view {
    width: 100%;
    transform-origin: top left;
    display: inline-block;
    /* 使缩放更可控，并配合容器滚动 */
}

.pdf-status {
    color: #8f959e;
    padding: 12px;
}

.pdf-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    /* 工具栏置于预览框上方 */
}

.zoom,
.page {
    color: #6b7280;
}

.pdf-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 8px;
}

.zoom,
.page {
    color: #6b7280;
}

.doc-info-panel {
    padding: 20px 0;
}

.docx-container {
    background: white;
    padding: 20px;
    border-radius: 4px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    min-height: 400px;
}

.docx-status {
    text-align: center;
    padding: 40px;
    color: #8f959e;
    font-size: 15px;
}

.pdf-status {
    text-align: center;
    padding: 40px;
    color: #8f959e;
    font-size: 15px;
}

// AI帮写样式
.ai-input-panel {
    background: #f6f8fa;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 16px;
}

.ai-input-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
}

.ai-title {
    font-weight: 600;
    color: #1f2329;
    display: flex;
    align-items: center;
}

.ai-input-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 12px;
}

.ai-control-panel {
    background: linear-gradient(135deg, #e8f5e9 0%, #f1f8e9 100%);
    border: 1px solid #67c23a;
    border-radius: 8px;
    padding: 12px 16px;
    margin-bottom: 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.ai-control-info {
    display: flex;
    align-items: center;
    color: #1f2329;
    font-weight: 500;
}

.ai-control-actions {
    display: flex;
    gap: 8px;
}
</style>
