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
                <div v-else id="vditor-container" class="editor-area"></div>
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
                <!-- Word 文件提示 -->
                <template v-else>
                    <div class="doc-info-panel">
                        <el-card>
                            <template #header>
                                <div style="display: flex; justify-content: space-between; align-items: center;">
                                    <span>📄 {{ knowledge.title }}</span>
                                    <el-button type="primary" size="small" @click="downloadFile">下载文件</el-button>
                                </div>
                            </template>
                            <el-alert title="Word 文件预览" type="info" description="此文件为 Word 文档。请点击下载按钮获取原文件进行查看。"
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
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getKnowledge, updateKnowledge, uploadFile, getKnowledgeFileBlob } from '@/api/knowledge'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import VuePdfEmbed from 'vue-pdf-embed'
import Vditor from 'vditor'
import 'vditor/dist/index.css'

const route = useRoute()
const knowledge = ref({})
const loading = ref(false)
const isEditing = ref(false)
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
        mode: 'sv',
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
const fileExtension = ref('')

const loadDocPreview = async () => {
    isPdf.value = false
    pdfError.value = ''
    pdfSrc.value = null
    pdfLoading.value = true

    try {
        const blob = await getKnowledgeFileBlob(knowledge.value.id)
        if (!blob || !(blob instanceof Blob)) {
            pdfError.value = '预览失败：未获取到文件数据'
            return
        }

        // 检测文件类型
        const title = knowledge.value.title || ''
        const mimeType = blob.type || ''

        if (mimeType.includes('pdf') || title.toLowerCase().endsWith('.pdf')) {
            // PDF 文件
            isPdf.value = true
            pdfSrc.value = URL.createObjectURL(blob)
        } else {
            // Word 文件 - 只显示提示
            isPdf.value = false
            const match = title.match(/\.([a-zA-Z0-9]+)$/)
            if (match) {
                fileExtension.value = match[1].toUpperCase()
            }
        }
    } catch (e) {
        console.error(e)
        pdfError.value = '加载文件失败'
    } finally {
        pdfLoading.value = false
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
</style>
