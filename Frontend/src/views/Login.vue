<template>
  <div class="login-page">
    <!-- 左侧视觉区 -->
    <div class="login-visual">
      <div class="visual-content">
        <div class="brand-text">
          <h1 class="typewriter">享记——<br>记你所想</h1>
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'
import { ElMessage } from 'element-plus'

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
}

.brand-text h1 {
  font-size: 4rem;
  font-weight: 800;
  line-height: 1.2;
  margin: 0;
  background: linear-gradient(to right, #fff, #e2e8f0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-family: 'Inter', sans-serif;
}

.typewriter {
  overflow: hidden;
  border-right: .15em solid orange;
  white-space: nowrap;
  margin: 0 auto;
  letter-spacing: .15em;
  animation:
    typing 3.5s steps(40, end),
    blink-caret .75s step-end infinite;
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
