<template>
  <div class="app-layout">
    <el-header class="app-header">
      <div class="header-left">
        <div class="logo" @click="$router.push('/')">
          <el-icon><Reading /></el-icon>
          <span>智能知识库</span>
        </div>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            <el-avatar :size="32" :src="userAvatar">{{ username.charAt(0).toUpperCase() }}</el-avatar>
            <span class="username">{{ username }}</span>
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="app-main">
      <router-view></router-view>
    </el-main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Reading, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const username = ref('User')
const userAvatar = ref('')

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    username.value = user.username || 'User'
  }
})

const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.app-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background-color: #fff;
  border-bottom: 1px solid #dee0e3;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  .logo {
    display: flex;
    align-items: center;
    font-size: 18px;
    font-weight: 600;
    color: #1f2329;
    cursor: pointer;
    .el-icon {
      margin-right: 8px;
      color: var(--el-color-primary);
    }
  }
}

.header-right {
  .el-dropdown-link {
    display: flex;
    align-items: center;
    cursor: pointer;
    color: #1f2329;
    .username {
      margin: 0 8px;
    }
  }
}

.app-main {
  padding: 0;
  flex: 1;
  background-color: #f5f6f7;
  overflow: hidden;
}
</style>
