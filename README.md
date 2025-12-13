## 1. 项目概述
本项目是一个使用SpringBoot3+Vue3构建的智能知识库，旨在为用户提供知识的增删改查以及基于大语言模型的RAG应用。

详细的技术栈与版本如下：

| 工具 | 版本 |
| --- | --- |
| Springboot | 3.1.5 |
| MyBatis | 3.0.3 |
| Vue | 3.0+ |
| JDK | 17 |
| MySQL | 8.0+ |
| UI库 | ElementPlus |
| 其他依赖详见pom.xml | |


仓库只有一条分支main，每次提交前先合并分支，推荐使用vscode进行git管理。

**接口文档->**[**https://s.apifox.cn/0ee97711-58b3-48cc-9940-7718b932847c/390794793e0**](https://s.apifox.cn/0ee97711-58b3-48cc-9940-7718b932847c/390794793e0)**（可以下载Apifox导入，在apifox对后端接口进行调试）**

**仓库->**[**https://github.com/CodePauler/KnowledgeBase**](https://github.com/CodePauler/KnowledgeBase)



### 如何启动项目？
1. 执行database/create_table.sql建表
2. 在IDEA导入项目并确定后端部分被正确识别为SpringBoot项目（或者使用maven命令自己编译）
3. 修改application.yml中的数据库用户名及密码
4. 在IDEA中点击运行按钮启动
5. url：http://localhost:8080

## 2. 命名规范
### 2.1 命名原则
+ **驼峰命名法**：所有变量、方法、类名、接口名等均采用驼峰命名法（类名第一个字母需大写，变量名第一个字母小写）。
    - 示例：`userName`, `getUserList`, `UserVo`, `userService`。
+ **语义清晰**：命名需具有明确的语义，便于理解变量或方法的功能。
+ **统一风格**：前后端命名风格保持一致。

### 2.2 前端命名规范
#### 2.2.1 组件命名
+ 组件名称采用`PascalCase`，模块化命名。
    - 示例：`UserList`, `LoginPage`, `HeaderComponent`。
+ 组件文件夹命名采用`kebab-case`。
    - 示例：`user-list`, `login-page`。

#### 2.2.2 变量命名
+ 数据变量采用`camelCase`。
    - 示例：`userName`, `userList`。
+ 组件props采用`camelCase`。
    - 示例：`userName`, `showModal`。

#### 2.2.3 方法命名
+ 方法名称需描述其功能。
    - 示例：`handleLogin`, `fetchData`, `showMessage`。

### 2.3 后端命名规范
#### 2.3.1 类命名
+ 类名称采用`PascalCase`。
    - 示例：`UserController`, `UserServiceImpl`, `UserMapper`。

#### 2.3.2 方法命名
+ 方法名称需描述其功能。
    - 示例：`getUserById`, `saveUser`, `deleteUser`。

#### 2.3.3 变量命名
+ 变量名称采用`camelCase`。
    - 示例：`userName`, `userList`。

#### 2.3.4 数据库表命名
+ 数据库表名采用`snake_case`。
    - 示例：`user_info`, `order_detail`。
+ 主键字段为`id`，外键字段为`<表名>_id`。
    - 示例：`user_id`。

---

## 3. 统一响应结果
### 3.1 响应格式
所有接口的响应结果统一包装成Result实体类

#### 3.1.1 响应成功
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

#### 3.1.2 响应失败
```json
{
  "code": 500,
  "message": "操作失败，服务器错误",
  "data": null
}
```

#### 3.1.3 分页响应
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 100,
    "pageSize": 10,
    "current": 1,
    "pages": 10,
    "records": [
      {
        "id": 1,
        "name": "张三"
      },
      ...
    ]
  }
}
```

### 3.2 响应状态码
| 状态码 | 描述 |
| --- | --- |
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未认证（未登录） |
| 403 | 权限不足 |
| 404 | 资源未找到 |
| 500 | 服务器内部错误 |


---

## 4. PageHelper使用规范
### 4.1 后端分页实现
+ 使用`PageHelper`进行分页查询，统一分页逻辑。
+ 分页参数传递：`pageNum`（当前页），`pageSize`（每页大小）。
+ 返回结果需包含分页信息：`total`（总数），`pages`（总页数），`records`（当前页数据）。

#### 4.1.1 示例代码
```java
// 后端分页查询
PageHelper.startPage(pageNum, pageSize);
List<User> userList = userMapper.selectAll();
PageInfo<User> pageInfo = new PageInfo<>(userList);
return pageInfo;
```

### 4.2 前端分页处理
+ 前端需根据分页信息展示分页组件。
+ 支持跳转到指定页和改变每页大小。

#### 4.2.1 示例代码
```vue
<template>
  <div>
    <el-pagination
      :current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      :pages="pages"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

```

---

## 5. 其他开发规范
+ mysql等的配置写在application.yml中，因为每个人账号密码不同，所以更新项目后注意一下数据库用户名和密码有没有变
+ 后端采用Controller-Service-ServiceImpl-Mapper架构，Controller接收请求，ServiceImpl实现Servicce接口，Mapper层进行数据库交互，所有SQL写在XML中（可以在IDEA里安装一个MyBatisX插件）
+ 通常Controller不直接返回Entity，而是封装一个Dto，只传输必要的数据。如果返回的内容前端用于展示，则封装成Vo
+ 接口均以/api开头

---

## 6. 代码风格
### 6.1 前端代码风格
+ Vue框架，ElementPlus组件库，可以让AI根据接口做

### 6.2 后端代码风格
+ **必要的地方加注释（如果有待改进/实现的地方，可以在注释里用TODO标出，比如//TODO: 登录需签发Token）**

---

## 7. 参考资料
**项目仓库**`https://github.com/CodePauler/KnowledgeBase`

**接口文档**`https://s.apifox.cn/0ee97711-58b3-48cc-9940-7718b932847c/390794793e0`

**Element Plus组件库**https://element-plus.org/zh-CN/component/overview

**SpringAi**`https://docs.spring.io/spring-ai/reference/index.html`

