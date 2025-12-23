<template>
    <div class="space-detail-layout">
        <div class="sidebar">
            <div class="sidebar-header">
                <el-button type="primary" class="full-width" @click="goToChat">
                    <el-icon>
                        <ChatDotRound />
                    </el-icon> AI 助手
                </el-button>
            </div>
            <div class="tree-container">
                <div class="tree-header">
                    <span>知识库</span>
                    <el-dropdown trigger="click" @command="handleCreate">
                        <el-icon class="add-icon">
                            <Plus />
                        </el-icon>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item command="folder">新建文件夹</el-dropdown-item>
                                <el-dropdown-item command="doc">新建文档</el-dropdown-item>
                                <el-dropdown-item command="upload">上传文件</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                </div>
                <el-tree :data="treeData" :props="defaultProps" @node-click="handleNodeClick" node-key="id"
                    default-expand-all highlight-current>
                    <template #default="{ node, data }">
                        <span class="custom-tree-node">
                            <el-icon v-if="data.type === 'FOLDER'">
                                <Folder />
                            </el-icon>
                            <el-icon v-else>
                                <Document />
                            </el-icon>
                            <span class="node-label">{{ node.label }}</span>
                        </span>
                    </template>
                </el-tree>
            </div>
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
                <el-form-item label="父节点" v-if="selectedNode && selectedNode.type === 'FOLDER'">
                    <span>{{ selectedNode.title }}</span>
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
            <el-upload class="upload-demo" drag action="#" :http-request="handleUpload" :limit="1">
                <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                <div class="el-upload__text">
                    Drop file here or <em>click to upload</em>
                </div>
            </el-upload>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getKnowledgeTree, createKnowledge, uploadKnowledgeFile, uploadFile } from '@/api/knowledge'
import { ChatDotRound, Plus, Folder, Document, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const spaceId = route.params.spaceId

const treeData = ref([])
const defaultProps = {
    children: 'children',
    label: 'title'
}

const createDialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const dialogTitle = ref('')
const createType = ref('')
const selectedNode = ref(null) // Currently selected node (for parentId)

const createForm = reactive({
    title: '',
    content: ''
})

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

const handleNodeClick = (data) => {
    selectedNode.value = data
    if (data.type === 'DOC' || data.type === 'MANUAL') {
        router.push({ name: 'document', params: { spaceId, docId: data.id } })
    }
}

const goToChat = () => {
    router.push({ name: 'chat', params: { spaceId } })
}

const handleCreate = (command) => {
    if (command === 'upload') {
        uploadDialogVisible.value = true
        return
    }
    createType.value = command === 'folder' ? 'FOLDER' : 'MANUAL'
    dialogTitle.value = command === 'folder' ? '新建文件夹' : '新建文档'
    createForm.title = ''
    createDialogVisible.value = true
}

const submitCreate = async () => {
    if (!createForm.title) {
        ElMessage.warning('请输入名称')
        return
    }

    const parentId = selectedNode.value && selectedNode.value.type === 'FOLDER' ? selectedNode.value.id : null

    const payload = {
        spaceId: spaceId,
        title: createForm.title,
        type: createType.value,
        parentId: parentId,
        content: '' // Empty for now
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

const handleUpload = async (options) => {
    const { file } = options
    const parentId = selectedNode.value && selectedNode.value.type === 'FOLDER' ? selectedNode.value.id : null

    try {
        // 1. Upload File to get blobKey
        const uploadRes = await uploadFile(file)
        if (uploadRes.code !== 200) {
            ElMessage.error(uploadRes.msg || '文件上传失败')
            return
        }
        const blobKey = uploadRes.data.blobKey

        // 2. Create Knowledge Entry with blobKey
        const createRes = await createKnowledge({
            spaceId: spaceId,
            title: file.name,
            type: 'DOC',
            parentId: parentId,
            blobKey: blobKey
        })

        if (createRes.code === 200) {
            ElMessage.success('上传成功')
            uploadDialogVisible.value = false
            fetchTree()
        } else {
            ElMessage.error(createRes.msg || '创建记录失败')
        }
    } catch (error) {
        console.error(error)
        ElMessage.error('操作失败')
    }
}

onMounted(() => {
    fetchTree()
})
</script>

<style scoped lang="scss">
.space-detail-layout {
    display: flex;
    height: 100%;
}

.sidebar {
    width: 260px;
    background-color: #fff;
    border-right: 1px solid #dee0e3;
    display: flex;
    flex-direction: column;
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
    font-size: 14px;
    font-weight: 600;
    color: #8f959e;

    .add-icon {
        cursor: pointer;

        &:hover {
            color: var(--el-color-primary);
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
    font-size: 14px;

    .el-icon {
        margin-right: 6px;
        color: #8f959e;
    }

    .node-label {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
}
</style>
