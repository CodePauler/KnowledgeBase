<template>
  <div class="login-page">
    <!-- 左侧视觉区 -->
    <div class="login-visual">
      <div class="visual-content">
        <div class="brand-text">
          <h1 class="title-line">
            {{ titleLine1 }}<span class="cursor" v-if="currentTyping === 'title1'">|</span>
          </h1>
          <h1 class="title-line">
            {{ titleLine2 }}<span class="cursor" v-if="currentTyping === 'title2'">|</span>
          </h1>
        </div>

        <!-- AI 帮写演示卡片 -->
        <div class="demo-card" :class="{ 'fade-in': showDemoCard }">
          <div class="demo-header">
            <el-icon>
              <EditPen />
            </el-icon>
            <span>AI 帮写</span>
          </div>
          <div class="demo-body">
            <div class="demo-input-area">
              <div class="demo-input-text">
                {{ demoInput }}<span class="cursor" v-if="currentTyping === 'input'">|</span>
              </div>
            </div>
            <div class="demo-output-area" v-if="showDemoOutput">
              <div v-if="isGenerating" class="generating-indicator">
                <el-icon class="is-loading">
                  <Loading />
                </el-icon>
                AI 思考中...
              </div>
              <div v-else class="markdown-body" v-html="renderedDemoOutput"></div>
              <span class="cursor" v-if="currentTyping === 'output'">|</span>
            </div>
          </div>
        </div>

        <div class="visual-shape"></div>
      </div>
    </div>

    <!-- 右侧功能区 -->
    <div class="login-form-container">
      <div class="form-wrapper">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>登录您的享记账号</p>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" size="large" class="login-form">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" class="custom-input"></el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password
              class="custom-input"></el-input>
          </el-form-item>

          <div class="form-actions">
            <el-button type="primary" class="action-btn login-btn" @click="handleLogin"
              :loading="loading">登录</el-button>
            <el-button class="action-btn register-btn" @click="$router.push('/register')">注册账号</el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { EditPen, Loading } from '@element-plus/icons-vue'
import { marked } from 'marked'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// ===== 打字机动画逻辑 =====
const titleLine1 = ref('')
const titleLine2 = ref('')
const demoInput = ref('')
const demoOutput = ref('')
const currentTyping = ref('title1') // title1, title2, input, output, done
const showDemoCard = ref(false)
const showDemoOutput = ref(false)
const isGenerating = ref(false)

const FULL_TITLE_1 = '享记     '
const FULL_TITLE_2 = '记你所想'
const FULL_INPUT = '帮我制定一个下周的学习计划，重点是复习Java和Vue3。'
const FULL_OUTPUT = `### 📅 下周学习计划

#### 周一：Java 基础巩固
- **上午**：复习集合框架 (List, Map, Set)
- **下午**：深入理解 Stream API 与 Lambda 表达式

#### 周二：Vue3 核心机制
- **上午**：Composition API 实战练习
- **下午**：Pinia 状态管理与 Vue Router 进阶`

const renderedDemoOutput = computed(() => {
  return marked(demoOutput.value)
})

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

const typeString = async (text, setter, speed = 100) => {
  for (let i = 0; i <= text.length; i++) {
    setter(text.slice(0, i))
    // 随机速度增加真实感
    await sleep(speed + Math.random() * 50)
  }
}

const startAnimation = async () => {
  // 1. 标题打字
  await sleep(500)
  await typeString(FULL_TITLE_1, (v) => titleLine1.value = v, 150)
  currentTyping.value = 'title2'
  await typeString(FULL_TITLE_2, (v) => titleLine2.value = v, 150)

  // 2. 显示演示卡片
  await sleep(800)
  showDemoCard.value = true
  await sleep(800) // 等待卡片淡入

  // 3. 演示输入
  currentTyping.value = 'input'
  await typeString(FULL_INPUT, (v) => demoInput.value = v, 80)

  // 4. 模拟思考
  await sleep(500)
  showDemoOutput.value = true
  isGenerating.value = true
  await sleep(1500)
  isGenerating.value = false

  // 5. 演示输出
  currentTyping.value = 'output'
  await typeString(FULL_OUTPUT, (v) => demoOutput.value = v, 30)

  // 结束
  currentTyping.value = 'done'
}

onMounted(() => {
  startAnimation()
})

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await login(form)
        if (res.code === 200) {
          ElMessage.success('登录成功')
          localStorage.setItem('token', res.data.token)
          localStorage.setItem('user', JSON.stringify(res.data))
          router.push('/')
        } else {
          ElMessage.error(res.msg || '登录失败')
        }
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
.login-page {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

/* 左侧视觉区 */
.login-visual {
  flex: 1;
  background: #0f172a;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  /* 改为纵向排列 */

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle at 50% 50%,
        rgba(76, 29, 149, 0.4),
        rgba(59, 130, 246, 0.4),
        rgba(236, 72, 153, 0.4),
        transparent 70%);
    animation: fluid-bg 15s ease-in-out infinite alternate;
    filter: blur(60px);
  }
}

.visual-content {
  position: relative;
  z-index: 10;
  padding: 40px;
  color: white;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  /* 左对齐 */
  width: 480px;
  /* 限制宽度以保持对齐 */
}

.brand-text {
  margin-bottom: 60px;
}

.title-line {
  font-size: 4rem;
  font-weight: 800;
  line-height: 1.2;
  margin: 0;
  background: linear-gradient(to right, #fff, #e2e8f0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-family: 'Inter', sans-serif;
  display: block;
  /* 确保换行 */
  min-height: 1.2em;
  /* 防止高度塌陷 */
}

.cursor {
  display: inline-block;
  width: 2px;
  background-color: orange;
  animation: blink 1s step-end infinite;
  margin-left: 4px;
  vertical-align: text-bottom;
  height: 1em;
}

@keyframes blink {

  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0;
  }
}

/* 演示卡片样式 */
.demo-card {
  width: 100%;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);

  &.fade-in {
    opacity: 1;
    transform: translateY(0);
  }
}

.demo-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-weight: 600;
  margin-bottom: 16px;
  font-size: 14px;
  opacity: 0.9;

  .el-icon {
    font-size: 16px;
  }
}

.demo-input-area {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  min-height: 44px;
  display: flex;
  align-items: center;
}

.demo-input-text {
  color: #e2e8f0;
  font-size: 14px;
  line-height: 1.5;
}

.demo-output-area {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  padding: 16px;
  min-height: 100px;
  color: #1e293b;
  font-size: 14px;
  line-height: 1.6;
  position: relative;

  .cursor {
    background-color: #1e293b;
    /* 深色背景下的光标颜色 */
    height: 14px;
    vertical-align: middle;
  }
}

.generating-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

/* Markdown 样式微调 */
.markdown-body {
  :deep(h3) {
    font-size: 16px;
    margin-top: 0;
    margin-bottom: 8px;
    color: #0f172a;
  }

  :deep(h4) {
    font-size: 14px;
    margin-top: 12px;
    margin-bottom: 6px;
    color: #334155;
  }

  :deep(ul) {
    padding-left: 20px;
    margin: 0;
  }

  :deep(li) {
    margin-bottom: 4px;
  }

  :deep(strong) {
    color: #2563eb;
  }
}

/* 右侧功能区 */
.login-form-container {
  width: 500px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-wrapper {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 40px;

  h2 {
    font-size: 32px;
    font-weight: 700;
    color: #1e293b;
    margin: 0 0 8px 0;
  }

  p {
    color: #64748b;
    font-size: 16px;
    margin: 0;
  }
}

.custom-input {
  :deep(.el-input__wrapper) {
    padding: 12px 16px;
    box-shadow: 0 0 0 1px #e2e8f0 inset;
    border-radius: 8px;

    &.is-focus {
      box-shadow: 0 0 0 2px var(--el-color-primary) inset;
    }
  }

  :deep(.el-input__inner) {
    height: 24px;
    font-size: 16px;
  }
}

.form-actions {
  display: flex;
  gap: 16px;
  margin-top: 32px;
}

.action-btn {
  flex: 1;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
}

.login-btn {
  background-color: #2563eb;
  border-color: #2563eb;

  &:hover {
    background-color: #1d4ed8;
    border-color: #1d4ed8;
  }
}

.register-btn {
  background: white;
  border: 1px solid #e2e8f0;
  color: #1e293b;

  &:hover {
    background: #f8fafc;
    border-color: #cbd5e1;
    color: #0f172a;
  }
}

@keyframes fluid-bg {
  0% {
    transform: rotate(0deg) scale(1);
  }

  50% {
    transform: rotate(180deg) scale(1.2);
  }

  100% {
    transform: rotate(360deg) scale(1);
  }
}

@keyframes typing {
  from {
    width: 0
  }

  to {
    width: 100%
  }
}

@keyframes blink-caret {

  from,
  to {
    border-color: transparent
  }

  50% {
    border-color: orange
  }
}
</style>
