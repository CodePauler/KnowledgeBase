<template>
  <div class="chat-message" :class="{ 'is-user': isUser }">
    <div class="avatar">
      <el-avatar :size="36" :class="isUser ? 'user-avatar' : 'ai-avatar'">
        <el-icon v-if="isUser"><UserFilled /></el-icon>
        <el-icon v-else><Service /></el-icon>
      </el-avatar>
    </div>
    <div class="message-content-wrapper">
      <div class="message-content">
        <div v-if="loading" class="typing-indicator">
          <span></span><span></span><span></span>
        </div>
        <div v-else class="markdown-body" v-html="renderedContent"></div>
      </div>
      <div v-if="sources && sources.length > 0" class="sources-list">
        <div class="sources-title">参考来源:</div>
        <div v-for="(source, index) in sources" :key="index" class="source-item" @click="$emit('click-source', source)">
          <span class="source-index">{{ index + 1 }}</span>
          <span class="source-title">{{ source.title }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import { UserFilled, Service } from '@element-plus/icons-vue'

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  role: {
    type: String,
    default: 'user' // user or assistant
  },
  loading: {
    type: Boolean,
    default: false
  },
  sources: {
    type: Array,
    default: () => []
  }
})

const isUser = computed(() => props.role === 'user')

const renderedContent = computed(() => {
  if (!props.content) return ''
  return marked(props.content, {
    highlight: (code, lang) => {
      const language = hljs.getLanguage(lang) ? lang : 'plaintext'
      return hljs.highlight(code, { language }).value
    }
  })
})
</script>

<style scoped lang="scss">
.chat-message {
  display: flex;
  margin-bottom: 24px;
  &.is-user {
    flex-direction: row-reverse;
    .message-content {
      background-color: var(--el-color-primary);
      color: #fff;
      border-radius: 12px 12px 0 12px;
    }
    .message-content-wrapper {
      align-items: flex-end;
    }
  }
  &:not(.is-user) {
    .message-content {
      background-color: #f5f6f7;
      color: #1f2329;
      border-radius: 0 12px 12px 12px;
    }
  }
}

.avatar {
  margin: 0 12px;
  .user-avatar {
    background-color: var(--el-color-primary);
  }
  .ai-avatar {
    background-color: #00b96b;
  }
}

.message-content-wrapper {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.message-content {
  padding: 12px 16px;
  font-size: 15px;
  line-height: 1.6;
  word-break: break-word;
  
  :deep(p) {
    margin: 0 0 8px;
    &:last-child {
      margin-bottom: 0;
    }
  }
  :deep(pre) {
    background: rgba(0,0,0,0.1);
    padding: 8px;
    border-radius: 4px;
    overflow-x: auto;
  }
}

.sources-list {
  margin-top: 8px;
  font-size: 12px;
  .sources-title {
    color: #8f959e;
    margin-bottom: 4px;
  }
  .source-item {
    display: inline-flex;
    align-items: center;
    background-color: #f5f6f7;
    padding: 4px 8px;
    border-radius: 4px;
    margin-right: 8px;
    margin-bottom: 4px;
    cursor: pointer;
    color: var(--el-color-primary);
    &:hover {
      background-color: #e1eaff;
    }
    .source-index {
      font-weight: bold;
      margin-right: 4px;
    }
    .source-title {
      max-width: 150px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.typing-indicator {
  display: flex;
  align-items: center;
  height: 24px;
  span {
    display: inline-block;
    width: 6px;
    height: 6px;
    background-color: #8f959e;
    border-radius: 50%;
    margin: 0 2px;
    animation: typing 1.4s infinite ease-in-out both;
    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
