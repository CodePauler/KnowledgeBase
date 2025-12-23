<template>
  <div class="auth-container flex-center">
    <el-card class="auth-card">
      <template #header>
        <div class="auth-header">
          <h2>智能知识库</h2>
          <p>登录</p>
        </div>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="full-width" @click="handleLogin" :loading="loading">登录</el-button>
        </el-form-item>
        <div class="auth-footer">
          <router-link to="/register">没有账号？去注册</router-link>
        </div>
      </el-form>
    </el-card>
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
.auth-container {
  height: 100vh;
  background-color: #f5f6f7;
}

.auth-card {
  width: 400px;
}

.auth-header {
  text-align: center;
  h2 {
    margin: 0;
    color: var(--el-color-primary);
  }
  p {
    margin: 10px 0 0;
    color: #606266;
  }
}

.full-width {
  width: 100%;
}

.auth-footer {
  text-align: center;
  margin-top: 10px;
  a {
    color: var(--el-color-primary);
    text-decoration: none;
    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
