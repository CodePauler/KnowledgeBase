<template>
    <div class="space-detail-layout">
        <div class="sidebar" :style="{ width: sidebarWidth + 'px' }">
            <div class="sidebar-header">
                <el-button type="primary" class="full-width" @click="goToChat">
                    <el-icon>
                        <ChatDotRound />
                    </el-icon> AI 助手
                </el-button>
            </div>
            <div class="tree-container">
                <div class="tree-header">
                    <span class="tree-title">知识库</span>
                    <div class="tree-actions">
                        <el-dropdown trigger="click" @command="handleCreate">
                            <el-icon class="add-icon" title="在根级创建">
                                <Plus />
                            </el-icon>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item command="doc">新建文档</el-dropdown-item>
                                    <el-dropdown-item command="upload">上传文件</el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
                    </div>
                </div>
                <el-tree ref="treeRef" :data="treeData" :props="defaultProps" @node-click="handleNodeClick"
                    node-key="id" highlight-current>
                    <template #default="{ node, data }">
                        <span class="custom-tree-node" @click.stop="handleNodeClick(data)">
                            <span class="node-left">
                                <span class="file-icon">
                                    <component :is="iconForNode(data)" />
                                </span>
                                <span class="node-label">{{ node.label }}</span>
                            </span>
                            <span class="node-actions">
                                <el-dropdown trigger="click" @command="cmd => handleCreateChildFor(cmd, data)">
                                    <el-icon class="add-icon" title="在该知识下创建">
                                        <Plus />
                                    </el-icon>
                                    <template #dropdown>
                                        <el-dropdown-menu>
                                            <el-dropdown-item command="child-doc">新建子文档</el-dropdown-item>
                                            <el-dropdown-item command="child-upload">上传子文件</el-dropdown-item>
                                            <el-dropdown-item divided command="delete">删除知识</el-dropdown-item>
                                        </el-dropdown-menu>
                                    </template>
                                </el-dropdown>
                            </span>
                        </span>
                    </template>
                </el-tree>
            </div>
            <div class="sidebar-resizer" @mousedown="startResize"></div>
        </div>
        <div class="content-area">
            <router-view :key="$route.fullPath"></router-view>
        </div>

        <!-- Create Dialogs -->
        <el-dialog v-model="createDialogVisible" :title="dialogTitle" width="30%">
            <el-form :model="createForm" label-position="top">
                <el-form-item label="名称">
                    <el-input v-model="createForm.title" autocomplete="off"></el-input>
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="createDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="submitCreate">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <el-dialog v-model="uploadDialogVisible" title="上传文件" width="30%">
            <el-form label-position="top" class="upload-form">
                <el-form-item label="知识名称">
                    <el-input v-model="uploadTitle"
                        :placeholder="uploadedFile ? uploadedFile.name : '请输入知识名称（默认为文件名）'" />
                </el-form-item>
            </el-form>
            <el-upload ref="uploadRef" class="upload-demo" drag action="#" :auto-upload="false" :limit="1"
                @change="onFileSelect">
                <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                <div class="el-upload__text">
                    Drop file here or <em>click to upload</em>
                </div>
                <template #tip v-if="uploadedFile">
                    <div style="margin-top: 8px; color: #606266;">
                        已选择: {{ uploadedFile.name }}
                    </div>
                </template>
            </el-upload>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="uploadDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="submitUpload">确认创建</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted, reactive, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getKnowledgeTree, createKnowledge, uploadFile, deleteKnowledge } from '@/api/knowledge'
import { ChatDotRound, Plus, Document, UploadFilled } from '@element-plus/icons-vue'
import MarkdownIcon from '@/components/icons/MarkdownIcon.vue'
import FileIcon from '@/components/icons/FileIcon.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const spaceId = route.params.spaceId

const treeData = ref([])
const defaultProps = {
    children: 'children',
    label: 'title'
}
const currentNode = ref(null)
const sidebarWidth = ref(parseInt(localStorage.getItem('sidebarWidth') || '320', 10))
const treeRef = ref(null)

const createDialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const dialogTitle = ref('')
const uploadRef = ref(null)
const uploadTitle = ref('')
const uploadedFile = ref(null) // 保存选中的文件

const submitUpload = () => {
    if (!uploadedFile.value) {
        ElMessage.warning('请先选择文件')
        return
    }
    // 直接触发上传逻辑
    handleUploadConfirm()
}

const handleUploadConfirm = async () => {
    if (!uploadedFile.value) {
        ElMessage.warning('请先选择文件')
        return
    }

    try {
        // 1. 上传文件获取 blobKey
        ElMessage.info('正在上传文件...')
        const uploadRes = await uploadFile(uploadedFile.value)
        if (uploadRes.code !== 200) {
            ElMessage.error(uploadRes.msg || '文件上传失败')
            return
        }
        const blobKey = uploadRes.data.blobKey
        ElMessage.success('文件上传成功')

        // 2. 创建知识记录
        ElMessage.info('正在创建知识...')
        const createRes = await createKnowledge({
            spaceId: spaceId,
            title: uploadTitle.value || uploadedFile.value.name,
            type: 'DOC',
            blobKey: blobKey,
            ...(isChildUpload.value && currentNode.value ? { parentId: currentNode.value.id } : {})
        })

        if (createRes.code === 200) {
            ElMessage.success('创建成功')
            uploadDialogVisible.value = false
            isChildUpload.value = false
            // 清空状态
            if (uploadRef.value) {
                uploadRef.value.clearFiles()
            }
            uploadTitle.value = ''
            uploadedFile.value = null
            fetchTree()
        } else {
            ElMessage.error(createRes.msg || '创建知识失败')
        }
    } catch (error) {
        console.error(error)
        ElMessage.error('操作失败')
    }
}

// 文件选择时的处理
const onFileSelect = (file, fileList) => {
    if (fileList.length > 0) {
        uploadedFile.value = fileList[0].raw
        // 自动填充知识名称（如果未输入）
        if (!uploadTitle.value) {
            uploadTitle.value = uploadedFile.value.name
        }
    } else {
        uploadedFile.value = null
    }
}

const createForm = reactive({
    title: '',
    content: ''
})
const createFormParentId = ref(null)
const isChildUpload = ref(false)

const fetchTree = async () => {
    try {
        const res = await getKnowledgeTree(spaceId)
        if (res.code === 200) {
            treeData.value = res.data
        }
    } catch (error) {
        console.error(error)
    }
}

// 提供给子组件使用
provide('refreshTree', fetchTree)

const handleNodeClick = (data) => {
    currentNode.value = data
    if (data.type === 'DOC' || data.type === 'MANUAL') {
        router.push({ name: 'document', params: { spaceId, docId: data.id } })
    }
}

// 控制树节点展开/折叠
const toggleNodeExpand = (node, data) => {
    if (!treeRef.value) return
    treeRef.value.store.nodesMap[data.id].expanded = !treeRef.value.store.nodesMap[data.id].expanded
}

const goToChat = () => {
    router.push({ name: 'chat', params: { spaceId } })
}

const handleCreate = (command) => {
    if (command === 'upload') {
        uploadDialogVisible.value = true
        uploadedFile.value = null
        uploadTitle.value = ''
        isChildUpload.value = false
        if (uploadRef.value) {
            uploadRef.value.clearFiles()
        }
        return
    }
    dialogTitle.value = '新建文档'
    createForm.title = ''
    createFormParentId.value = null
    createDialogVisible.value = true
}

const handleCreateChildFor = (command, parent) => {
    if (!parent) return
    if (command === 'delete') {
        return handleDeleteKnowledge(parent)
    }
    if (command === 'child-upload') {
        uploadDialogVisible.value = true
        isChildUpload.value = true
        currentNode.value = parent
        uploadedFile.value = null
        uploadTitle.value = ''
        if (uploadRef.value) {
            uploadRef.value.clearFiles()
        }
        return
    }
    dialogTitle.value = '新建子文档'
    createForm.title = ''
    createFormParentId.value = parent.id
    createDialogVisible.value = true
}

const submitCreate = async () => {
    if (!createForm.title) {
        ElMessage.warning('请输入名称')
        return
    }

    const payload = {
        spaceId: spaceId,
        title: createForm.title,
        type: 'MANUAL',
        content: '' // Empty for now
    }
    if (createFormParentId.value) {
        payload.parentId = createFormParentId.value
    }

    try {
        const res = await createKnowledge(payload)
        if (res.code === 200) {
            ElMessage.success('创建成功')
            createDialogVisible.value = false
            fetchTree()
        } else {
            ElMessage.error(res.msg || '创建失败')
        }
    } catch (error) {
        console.error(error)
    }
}


// 删除指定知识
const handleDeleteKnowledge = async (data) => {
    try {
        await ElMessageBox.confirm(`确定删除“${data.title}”吗？此操作不可恢复。`, '删除确认', {
            confirmButtonText: '删除',
            cancelButtonText: '取消',
            type: 'warning'
        })
        const res = await deleteKnowledge(data.id)
        if (res.code === 200) {
            ElMessage.success('删除成功')
            router.push({ name: 'space-home', params: { spaceId } })
            fetchTree()
        } else {
            ElMessage.error(res.msg || '删除失败')
        }
    } catch (e) {
        // 用户取消或请求异常
        if (e !== 'cancel') console.error(e)
    }
}

onMounted(() => {
    fetchTree()
})

// 侧栏拖拽伸缩
let resizeStartX = 0
let resizeStartWidth = 0
const startResize = (e) => {
    resizeStartX = e.clientX
    resizeStartWidth = sidebarWidth.value
    document.addEventListener('mousemove', onResizing)
    document.addEventListener('mouseup', stopResize)
}
const onResizing = (e) => {
    const delta = e.clientX - resizeStartX
    const next = Math.min(600, Math.max(240, resizeStartWidth + delta))
    sidebarWidth.value = next
}
const stopResize = () => {
    document.removeEventListener('mousemove', onResizing)
    document.removeEventListener('mouseup', stopResize)
    localStorage.setItem('sidebarWidth', String(sidebarWidth.value))
}

// 仅区分“手工类(MANUAL)”与“文件类(DOC)”
const iconForNode = (data) => {
    if (data.type === 'MANUAL') return MarkdownIcon
    return FileIcon
}
</script>

<style scoped lang="scss">
.space-detail-layout {
    display: flex;
    height: 100%;
}

.sidebar {
    /* 宽度通过响应式绑定控制 */
    background-color: #fff;
    border-right: 1px solid #dee0e3;
    display: flex;
    flex-direction: column;
    position: relative;
}

.sidebar-header {
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;
}

.full-width {
    width: 100%;
}

.tree-container {
    flex: 1;
    overflow-y: auto;
    padding: 10px 0;
}

.tree-header {
    padding: 0 16px 10px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 16px;
    font-weight: 600;
    color: #1f2329;

    .tree-title {
        font-size: 18px;
        font-weight: 700;
    }

    .tree-actions {
        display: flex;
        align-items: center;
        gap: 8px;
    }

    .add-icon {
        cursor: pointer;

        &:hover {
            color: var(--el-color-primary);
        }

        &.disabled {
            opacity: 0.4;
            cursor: not-allowed;
        }
    }
}

.content-area {
    flex: 1;
    overflow: hidden;
    background-color: #fff;
}

.custom-tree-node {
    display: flex;
    align-items: center;
    justify-content: space-between;
    /* 左右分布：左侧信息 + 右侧操作 */
    font-size: 18px;
    padding: 8px 16px;
    /* 增加垂直内边距 */
    min-height: 40px;
    /* 增加单条节点的垂直高度 */
    width: 100%;
    /* 让节点块占满可用宽度，便于右对齐 */

    .el-icon {
        margin-right: 6px;
        color: #8f959e;
    }

    .node-left {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
        flex: 1;
        /* 推动右侧操作贴近右边 */
        /* 允许内容收缩以正确溢出省略 */
    }

    .node-label {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    .file-icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        margin-right: 6px;
        width: 18px;
        height: 18px;
    }

    .node-actions {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        margin-left: 12px;
        flex-shrink: 0;
        /* 保持右侧操作不被挤压隐藏 */
    }
}

:deep(.el-tree) {
    padding-right: 8px;
    /* 为右侧操作留出一些空间 */
}

.sidebar-resizer {
    position: absolute;
    top: 0;
    right: -4px;
    width: 8px;
    height: 100%;
    cursor: col-resize;
}

/* 调整 Element Plus Tree 节点的默认内容高度与内边距 */
:deep(.el-tree-node__content) {
    padding: 0;
    /* 让自定义节点块控制内边距与高度 */
    height: auto;
}
</style>
