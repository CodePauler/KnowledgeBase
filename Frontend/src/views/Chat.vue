<template>
  <div class="chat-container">
    <div class="chat-header">
      <div class="header-title">AI 助手</div>
      <div class="header-actions">
        <el-button circle @click="clearHistory">
          <el-icon>
            <Delete />
          </el-icon>
        </el-button>
        <el-button circle @click="settingsVisible = true">
          <el-icon>
            <Setting />
          </el-icon>
        </el-button>
      </div>
    </div>

    <div class="chat-history" ref="chatHistoryRef">
      <div v-if="messages.length === 0" class="empty-chat">
        <el-icon size="48" color="#dee0e3">
          <ChatDotRound />
        </el-icon>
        <p>你好！我是你的智能知识库助手，请问有什么可以帮你的吗？</p>
      </div>
      <ChatMessage v-for="(msg, index) in messages" :key="index" :role="msg.role" :content="msg.content"
        :loading="msg.loading" :sources="msg.sources" @click-source="handleSourceClick" />
    </div>

    <div class="chat-input-area">
      <div class="input-wrapper">
        <el-input v-model="input" type="textarea" :rows="3" placeholder="请输入你的问题... (Enter 发送, Shift + Enter 换行)"
          resize="none" @keydown.enter.prevent="handleEnter" />
        <div class="input-actions">
          <el-button type="primary" :loading="sending" @click="sendMessage" :disabled="!input.trim()">
            <el-icon>
              <Position />
            </el-icon> 发送
          </el-button>
        </div>
      </div>
    </div>

    <el-drawer v-model="settingsVisible" title="对话设置" size="300px">
      <el-form label-position="top">
        <el-form-item label="Top K (引用数量)">
          <el-slider v-model="settings.topK" :min="1" :max="10" show-input />
        </el-form-item>
        <el-form-item label="相似度阈值">
          <el-slider v-model="settings.similarityThreshold" :min="0" :max="1" :step="0.1" show-input />
        </el-form-item>
        <el-form-item label="单文档片段数">
          <el-slider v-model="settings.chunksPerKnowledge" :min="1" :max="5" show-input />
        </el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { chatStream } from '@/api/chat'
import ChatMessage from '@/components/ChatMessage.vue'
import { Delete, Setting, ChatDotRound, Position } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const spaceId = route.params.spaceId

const messages = ref([])
const input = ref('')
const sending = ref(false)
const chatHistoryRef = ref(null)
const settingsVisible = ref(false)
const conversationId = ref('')

const settings = reactive({
  topK: 3,
  similarityThreshold: 0.4,
  chunksPerKnowledge: 3
})

const scrollToBottom = () => {
  nextTick(() => {
    if (chatHistoryRef.value) {
      chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight
    }
  })
}

const handleEnter = (e) => {
  if (e.shiftKey) return
  sendMessage()
}

const sendMessage = async () => {
  const text = input.value.trim()
  if (!text || sending.value) return

  input.value = ''
  sending.value = true

  // Add user message
  messages.value.push({
    role: 'user',
    content: text
  })
  scrollToBottom()

  // Add AI placeholder
  const aiMessage = reactive({
    role: 'assistant',
    content: '',
    loading: true,
    sources: []
  })
  messages.value.push(aiMessage)
  scrollToBottom()

  try {
    await chatStream(
      {
        question: text,
        spaceId: spaceId,
        conversationId: conversationId.value,
        topK: settings.topK,
        similarityThreshold: settings.similarityThreshold,
        chunksPerKnowledge: settings.chunksPerKnowledge
      },
      (chunk) => {
        if (aiMessage.loading) aiMessage.loading = false
        aiMessage.content += chunk
        scrollToBottom()
      },
      (error) => {
        console.error(error)
        aiMessage.loading = false
        aiMessage.content += '\n[出错了，请稍后再试]'
      },
      () => {
        aiMessage.loading = false
        // Stream completed
      }
    )
  } catch (e) {
    console.error(e)
    aiMessage.loading = false
    aiMessage.content = '发送失败'
  } finally {
    sending.value = false
  }
}

const clearHistory = () => {
  messages.value = []
  conversationId.value = '' // Reset conversation
}

const handleSourceClick = (source) => {
  // Navigate to document
  router.push({ name: 'document', params: { spaceId, docId: source.knowledgeId } })
}
</script>

<style scoped lang="scss">
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #fff;
  position: relative;
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid #f1f2f5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  z-index: 10;

  .header-title {
    font-size: 16px;
    font-weight: 600;
    color: #1f2329;
  }
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  padding-bottom: 140px;
  /* Space for floating input */
  scroll-behavior: smooth;

  /* Center content */
  .chat-message {
    max-width: 800px;
    margin-left: auto;
    margin-right: auto;
  }
}

.empty-chat {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #94a3b8;

  p {
    margin-top: 16px;
    font-size: 16px;
  }
}

.chat-input-area {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  width: 90%;
  max-width: 800px;
  z-index: 20;
}

.input-wrapper {
  position: relative;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;

  &:focus-within {
    border-color: #2563eb;
    box-shadow: 0 4px 24px rgba(37, 99, 235, 0.12);
  }

  :deep(.el-textarea__inner) {
    border: none;
    padding: 0;
    box-shadow: none;
    resize: none;
    max-height: 200px;
    font-size: 16px;
    line-height: 1.6;
    background: transparent;
  }
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
}
</style>
