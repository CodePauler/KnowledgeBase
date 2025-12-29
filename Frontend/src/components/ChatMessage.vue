<template>
  <div class="chat-message" :class="{ 'is-user': isUser }">
    <div class="avatar">
      <el-avatar :size="36" :class="isUser ? 'user-avatar' : 'ai-avatar'">
        <el-icon v-if="isUser">
          <UserFilled />
        </el-icon>
        <el-icon v-else>
          <Service />
        </el-icon>
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

// 规范化 markdown 格式
const normalizeMarkdown = (text) => {
  if (!text) return ''

  let output = text

  // 1. 【基础结构修复】
  // 修复标题：#后没空格 (如 "#标题" -> "# 标题")
  output = output.replace(/^(#{1,6})([^\s#])/gm, '$1 $2')

  // 修复引用块：>后没空格 (如 ">内容" -> "> 内容")
  output = output.replace(/^>([^\s])/gm, '> $1')

  // 修复列表：-或*或数字后没空格
  output = output.replace(/^(\s*[-*]|\d+\.)([^\s])/gm, '$1 $2')

  // 2. 【代码块修复】(最容易炸裂的地方)
  // 场景：代码块 ``` 前面没有换行，导致渲染器识别为普通文本
  // 修复前：文字内容```java
  // 修复后：文字内容\n\n```java
  output = output.replace(/([^\n])\s*(```)/g, '$1\n\n$2')

  // 场景：代码块结束 ``` 后面没有换行，导致紧接的文字被吞
  // 修复前：```\n文字内容
  // 修复后：```\n\n文字内容
  output = output.replace(/(```)\s*([^\n]*?)\s*([^\n`])/g, (match, p1, p2, p3) => {
    // 这里逻辑稍微复杂，为了防止误伤代码块内部的内容，
    // 我们主要针对代码块闭合标记后紧跟文字的情况
    // 但用正则很难完美区分闭合还是开启，建议只做上面的“开启前换行”通常就够了
    return match
  })

  // 3. 【表格修复】
  // 很多 Markdown 解析器要求表格前后必须有空行，否则不渲染
  // 查找类似 | header | header | 的行，如果前面不是换行符，加一个
  output = output.replace(/([^\n])\n(\|.*\|)\n/g, '$1\n\n$2\n')

  // 4. 【粗体/斜体优化】
  // 修复粗体内部的多余空格：** 文字  ** -> **文字**
  output = output.replace(/\*\*\s+(.*?)\s+\*\*/g, '**$1**')

  // 5. 【LaTeX 公式修复】(针对智谱/OpenAI 常见的格式差异)
  // 许多 Web 渲染组件(如 react-markdown) 不支持 \[ \] 换行格式，强制转换为 $$
  output = output
    .replace(/\\\[/g, '$$') // 将 \[ 替换为 $$
    .replace(/\\\]/g, '$$') // 将 \] 替换为 $$
    .replace(/\\\(/g, '$')  // 将 \( 替换为 $
    .replace(/\\\)/g, '$')  // 将 \) 替换为 $

  // 6. 【去除 Frontmatter 风格的分隔符】
  // 有些模型喜欢在回答首尾加上 ---
  if (output.trim().startsWith('---')) {
    output = output.replace(/^\s*---\s*\n/, '')
  }
  if (output.trim().endsWith('---')) {
    output = output.replace(/\n\s*---\s*$/, '')
  }

  return output
}

const renderedContent = computed(() => {
  if (!props.content) return ''
  const normalized = normalizeMarkdown(props.content)
  return marked(normalized, {
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
    background: rgba(0, 0, 0, 0.1);
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

    &:nth-child(1) {
      animation-delay: -0.32s;
    }

    &:nth-child(2) {
      animation-delay: -0.16s;
    }
  }
}

@keyframes typing {

  0%,
  80%,
  100% {
    transform: scale(0);
  }

  40% {
    transform: scale(1);
  }
}
</style>
