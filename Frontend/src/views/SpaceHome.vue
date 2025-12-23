<template>
  <div class="space-home flex-center">
    <div class="empty-state">
      <el-icon size="64" color="#dee0e3"><Folder /></el-icon>
      <p>选择左侧文档查看，或点击“AI 助手”开始对话</p>
    </div>
  </div>
</template>

<script setup>
import { Folder } from '@element-plus/icons-vue'
</script>

<style scoped>
.space-home {
  height: 100%;
  color: #8f959e;
}
.empty-state {
  text-align: center;
}
</style>
