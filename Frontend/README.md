# 智能知识库前端 (Frontend)

基于 Vue 3 + Vite + Element Plus 构建的现代化知识库前端应用。

## 1. 项目结构

```
Frontend/
├── src/
│   ├── api/            # 后端接口封装
│   ├── components/     # 公共组件 (如 ChatMessage)
│   ├── router/         # 路由配置
│   ├── styles/         # 全局样式 (SCSS)
│   ├── utils/          # 工具库 (Axios 封装)
│   ├── views/          # 页面视图
│   │   ├── Login.vue       # 登录页
│   │   ├── Register.vue    # 注册页
│   │   ├── Layout.vue      # 主布局
│   │   ├── SpaceList.vue   # 空间列表
│   │   ├── SpaceDetail.vue # 空间详情 (侧边栏 + 内容区)
│   │   ├── Document.vue    # 文档查看/编辑
│   │   └── Chat.vue        # AI 对话界面
│   ├── App.vue         # 根组件
│   └── main.js         # 入口文件
├── index.html          # HTML 模板
├── package.json        # 依赖配置
└── vite.config.js      # Vite 配置 (包含 API 代理)
```

## 2. 快速开始

### 2.1 环境准备

确保已安装 Node.js (推荐 v16+)。

### 2.2 安装依赖

在 `Frontend` 目录下运行：

```bash
npm install
```

### 2.3 启动开发服务器

```bash
npm run dev
```

启动后，访问控制台输出的地址 (通常是 `http://localhost:5173`)。

**注意**: 请确保后端服务已启动并在 `8080` 端口运行，否则 API 请求将失败。

## 3. 功能特性

- **用户认证**: 登录、注册 (JWT)。
- **空间管理**: 创建、查看、删除知识空间。
- **知识管理**:
  - 树状结构展示文档和文件夹。
  - 支持 Markdown 文档的在线编辑与预览。
  - 支持文件上传 (后端存储到 Azure Blob/Local)。
- **AI 助手**:
  - 基于 RAG (检索增强生成) 的智能问答。
  - 支持流式对话 (SSE)，实时打字机效果。
  - 可配置检索参数 (TopK, 阈值等)。
  - (未来支持) 显示引用来源。

## 4. 技术栈

- **Vue 3**: Composition API
- **Vite**: 构建工具
- **Element Plus**: UI 组件库
- **Axios**: HTTP 请求
- **Vue Router**: 路由管理
- **Marked + Highlight.js**: Markdown 渲染与代码高亮
- **Sass**: CSS 预处理
