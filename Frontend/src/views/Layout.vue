<template>
  <div class="app-layout">
    <el-header class="app-header">
      <div class="header-left">
        <div class="logo" @click="$router.push('/')">
          <el-icon>
            <Reading />
          </el-icon>
          <span>享记</span>
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
  border-bottom: 1px solid #f1f2f5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 56px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  z-index: 100;
}

.header-left {
  .logo {
    display: flex;
    align-items: center;
    font-size: 20px;
    font-weight: 700;
    color: #1f2329;
    cursor: pointer;
    letter-spacing: -0.5px;

    .el-icon {
      margin-right: 8px;
      color: #2563eb;
      font-size: 24px;
    }
  }
}

.header-right {
  .el-dropdown-link {
    display: flex;
    align-items: center;
    cursor: pointer;
    color: #1f2329;
    padding: 4px 8px;
    border-radius: 6px;
    transition: background-color 0.2s;

    &:hover {
      background-color: #f5f6f7;
    }

    .username {
      margin: 0 8px;
      font-weight: 500;
      font-size: 14px;
    }
  }
}

.app-main {
  padding: 0;
  flex: 1;
  background-color: #fff;
  overflow: hidden;
}
</style>
