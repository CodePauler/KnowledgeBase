<template>
  <div class="space-list-container">
    <div class="space-header">
      <h2>我的空间</h2>
      <el-button type="primary" @click="dialogVisible = true">
        <el-icon>
          <Plus />
        </el-icon> 新建空间
      </el-button>
    </div>

    <div class="space-grid" v-loading="loading">
      <el-card v-for="space in spaces" :key="space.id" class="space-card" shadow="hover" @click="goToSpace(space.id)">
        <div class="space-card-header">
          <div class="space-icon">{{ space.name.charAt(0).toUpperCase() }}</div>
          <div class="space-info">
            <h3>{{ space.name }}</h3>
            <p>{{ space.description || '暂无描述' }}</p>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="dialogVisible" title="新建空间" width="30%">
      <el-form :model="form" label-position="top">
        <el-form-item label="空间名称">
          <el-input v-model="form.name" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateSpace">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSpaces, createSpace } from '@/api/space'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const spaces = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = reactive({
  name: '',
  description: ''
})

const fetchSpaces = async () => {
  loading.value = true
  try {
    const res = await getSpaces()
    if (res.code === 200) {
      spaces.value = res.data
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleCreateSpace = async () => {
  if (!form.name) {
    ElMessage.warning('请输入空间名称')
    return
  }
  try {
    const res = await createSpace(form)
    if (res.code === 200) {
      ElMessage.success('创建成功')
      dialogVisible.value = false
      form.name = ''
      form.description = ''
      fetchSpaces()
    } else {
      ElMessage.error(res.msg || '创建失败')
    }
  } catch (error) {
    console.error(error)
  }
}

const goToSpace = (id) => {
  router.push(`/space/${id}`)
}

onMounted(() => {
  fetchSpaces()
})
</script>

<style scoped lang="scss">
.space-list-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.space-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    color: #1f2329;
  }
}

.space-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.space-card {
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-2px);
  }
}

.space-card-header {
  display: flex;
  align-items: center;

  .space-icon {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    background-color: var(--el-color-primary);
    color: #fff;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 24px;
    font-weight: bold;
    margin-right: 16px;
  }

  .space-info {
    h3 {
      margin: 0 0 4px;
      font-size: 16px;
      color: #1f2329;
    }

    p {
      margin: 0;
      font-size: 14px;
      color: #8f959e;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}
</style>
