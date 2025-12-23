<template>
  <div class="document-container" v-loading="loading">
    <div class="doc-header">
      <div class="title-area">
         <h1 v-if="!isEditing">{{ knowledge.title }}</h1>
         <el-input v-else v-model="editForm.title" size="large" />
      </div>
      <div class="actions">
        <el-button v-if="!isEditing" @click="startEdit">编辑</el-button>
        <template v-else>
           <el-button @click="cancelEdit">取消</el-button>
           <el-button type="primary" @click="saveEdit">保存</el-button>
        </template>
        <el-button v-if="knowledge.type === 'DOC'" @click="downloadFile">下载原文件</el-button>
      </div>
    </div>
    
    <div class="doc-content">
       <div v-if="isEditing" class="editor-area">
          <el-input
             v-model="editForm.content"
             type="textarea"
             :rows="20"
             placeholder="请输入 Markdown 内容"
          />
       </div>
       <div v-else class="markdown-body" v-html="renderedContent"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getKnowledge, updateKnowledge, getKnowledgeFileUrl } from '@/api/knowledge'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const route = useRoute()
const knowledge = ref({})
const loading = ref(false)
const isEditing = ref(false)
const editForm = ref({
  title: '',
  content: ''
})

const fetchKnowledge = async () => {
  const id = route.params.docId
  if (!id) return
  loading.value = true
  try {
    const res = await getKnowledge(id)
    if (res.code === 200) {
      knowledge.value = res.data
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const renderedContent = computed(() => {
  if (!knowledge.value.content) return '<p>暂无内容</p>'
  return marked(knowledge.value.content, {
    highlight: (code, lang) => {
      const language = hljs.getLanguage(lang) ? lang : 'plaintext'
      return hljs.highlight(code, { language }).value
    }
  })
})

const startEdit = () => {
  editForm.value = {
    title: knowledge.value.title,
    content: knowledge.value.content || ''
  }
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
}

const saveEdit = async () => {
  try {
    const res = await updateKnowledge(knowledge.value.id, {
      title: editForm.value.title,
      content: editForm.value.content
    })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      knowledge.value = res.data
      isEditing.value = false
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (error) {
    console.error(error)
  }
}

const downloadFile = () => {
  window.open(getKnowledgeFileUrl(knowledge.value.id), '_blank')
}

watch(() => route.params.docId, () => {
  fetchKnowledge()
  isEditing.value = false
})

onMounted(() => {
  fetchKnowledge()
})
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

.markdown-body {
  /* Basic markdown styles */
  :deep(h1), :deep(h2), :deep(h3) {
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
</style>
