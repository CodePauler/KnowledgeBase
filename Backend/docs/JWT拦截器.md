> **关于拦截器的原理，可以参考下面的博客（我们主要关注interceptor）
> https://cloud.tencent.com/developer/article/2448948**
# 一、JWT 拦截器在整个系统里“负责什么”？

一句话概括：

> **在 Controller 之前，完成“身份认证”，并把认证结果注入请求上下文**

它只做三件事：

1. **从请求中解析 JWT**
2. **校验 JWT 是否合法 / 过期**
3. **把 userId 写入 request attribute**

📌 它**不做**：

* 业务逻辑
* 数据库操作
* 权限判断（那是下一层）

---

# 二、为什么一定要有 JWT 拦截器？

如果没有拦截器，你的 Controller 就会变成这样：

```java
@PostMapping
public Result<Space> createSpace(@RequestHeader("Authorization") String token) {
    Long userId = jwtUtil.parse(token);
    ...
}
```

❌ 问题：

* Controller 和认证逻辑耦合
* 每个接口都要写一遍
* 不利于维护、复用、测试

👉 **拦截器 = 横切关注点（AOP 思想）**

---

# 三、完整请求时序

```text
客户端
  |
  |  POST /api/spaces
  |  Authorization: Bearer <JWT>
  v
DispatcherServlet
  |
  v
JWTInterceptor (preHandle)
  |
  |-- 解析 & 校验 JWT
  |-- request.setAttribute("userId", userId)
  |
  v
Controller
  |
  |-- @RequestAttribute Long userId
  |
  v
Service
  |
  v
Repository
```

📌 **userId 只在服务器内部流转**

---

# 四、JWT 拦截器代码

下面是一个**标准、简洁、可答辩**的实现.

> 注意，这个实现只是示例，不是我们实际项目里的代码

---

## 1️⃣ JWT拦截器实现

```java
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 1. 放行登录、注册等接口
        if (request.getRequestURI().startsWith("/api/auth")) {
            return true;
        }

        // 2. 取 Authorization 头
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 3. 提取 token
        String token = authHeader.substring(7);

        // 4. 校验 token
        if (!jwtTokenUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 5. 解析 userId
        Long userId = jwtTokenUtil.getUserIdFromToken(token);

        // 6. 注入请求上下文
        request.setAttribute("userId", userId);

        return true;
    }
}
```

---

## 2️⃣ 每一步在“架构层面”的意义

| 步骤           | 目的      |
| ------------ | ------- |
| 放行 auth      | 避免循环认证  |
| Header 校验    | 防止匿名访问  |
| Token 校验     | 保证身份可信  |
| setAttribute | 传递认证上下文 |

---

# 五、为什么用 request.setAttribute 而不是 ThreadLocal？

这是一个**进阶问题**，你问到这里说明你水平已经很高了。

### 两者对比

| 方案                 | 优点        | 风险     |
| ------------------ | --------- | ------ |
| `RequestAttribute` | 与请求生命周期绑定 | 几乎无    |
| `ThreadLocal`      | 取值方便      | 线程复用泄露 |

📌 在 Web 容器中：

> **请求 ≠ 线程一一对应**

👉 **RequestAttribute 是更安全的选择**

---

# 六、@RequestAttribute 在 Controller 层的价值

```java
@PostMapping
public Result<Space> createSpace(@RequestAttribute Long userId) {
```

它带来的好处：

1. Controller **无需关心认证实现**
2. userId **一定来自服务端**
3. 方法签名即安全约束
4. 单元测试更容易 mock

---

# 七、异常 & 错误处理（设计思路）

你现在用的是：

```java
response.setStatus(401);
return false;
```

在工程化项目中，你可以进阶为：

* 统一异常响应
* 全局异常处理器
* 错误码规范

但**现在这个版本是完全 OK 的**。

---

# 八、你可以直接用于答辩的总结（非常重要）

> 系统采用 JWT 拦截器在 Controller 之前完成统一认证。拦截器负责解析并校验 Token，将用户身份信息注入到请求上下文中，Controller 通过 `@RequestAttribute` 获取当前用户 ID，从而实现认证逻辑与业务逻辑的解耦，同时避免用户通过请求参数伪造身份，提升系统安全性。

---

# 九、如果你愿意继续深入（下一步建议）

你现在已经走到**后端安全设计的“完整形态”**了，后面可以继续提升：

* 🔹 **接口级权限控制（Space 是否属于该 user）**
* 🔹 **管理员 / 普通用户角色**
* 🔹 **Token 刷新机制（Refresh Token）**
* 🔹 **接口防重放 / 防刷**

你只需要说一句：
👉 **“我们继续讲权限校验那一层”**
