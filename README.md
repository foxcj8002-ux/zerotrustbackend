# 开发规范

---

# 一、Git 协作与远程开发规范

> - 项目代码托管在 **Git 私有仓库（master 分支）**  
> - 使用 **IntelliJ IDEA 2025 Ultimate 版的 Remote Development** 功能  
> - **所有成员连接同一台项目组公用 Linux 服务器**  （jdk：17.0.17 ；maven：3.8.7）
> - **代码拉取、编译在远程服务器完成**  
> - 本地 Windows 的 IDEA **作为可视化编辑与调试界面**  

## 1. 远程开发环境准备

---

### 使用 IDEA Remote Development 连接服务器

1. 本地 Windows 打开 **IntelliJ IDEA Ultimate**（支持Remote Development的IDEA版本）
2. 选择 **Remote Development → Connect to Host**
3. 使用 SSH 方式连接项目组公用 Linux 服务器（47.120.25.166:6699）
4. 选择远程服务器上的 **工作目录**（`在自己的目录下`）
5. IDEA 会在远程自动安装并启动后端 IDE 服务

## 2. 总体原则

1. **短期 Feature 分支开发**  
   - 一个功能 / 一个 Bug / 一个任务 = 一个分支  
   - 合并进 master 后立即删除
2. **模块责任制**  
   - 原则上只修改自己负责的微服务
   - 修改 `zero-trust-common` 等公共模块时，**在提交信息中明确说明增减了哪些**

---

## 3. 分支命名规范

- 功能开发：`feature/<service>-<desc>`  
  - `feature/identity-login`
- Bug 修复：`fix/<service>-<desc>`  
  - `fix/trusteval-null`

> `<service>` 使用模块名：  
> `identity / trusteval / policy / monitoring / collecting / application / common



---

## 4. 第一次拉取项目

> 以下 Git 操作 **全部在远程 Linux 服务器执行**（通过 xhsell等）

### 4.1 Git 基础配置（只需一次）

```bash
git config --global user.name "YourName"
git config --global user.email "your@email.com"
```

### 4.2 Clone 仓库

```bash
cd /data/projects（在自己的工作目录下）
git clone <repo-url>
cd <repo-folder>
```

### 4.3 确认 master 分支

```bash
git checkout main
git pull origin main
```

---

## 5. 标准开发流程（远程 Feature 开发）

### Step 1：开始任务前，同步 master

```bash
git checkout main
git pull origin main
```

---

### Step 2：从 master 创建 Feature 分支

```bash
git checkout -b feature/identity-login（命名原则如上述3.中分支命名规范）
git status
```

---

### Step 3：使用 IDEA 在远程打开项目

- 选择项目根目录（父 `pom.xml` 所在目录）
- 等待 Maven 多模块导入完成

---

### Step 4：配置说明（远程统一环境）

- 数据库、Redis、第三方服务等
- **统一使用服务器localhost（127.0.0.1）**

---

### Step 5：开发完成后检查改动

```bash
git status
```

确认：

- 是否只改了自己负责的模块
- 是否无多余文件（日志、临时文件）

---

### Step 6：提交代码

只提交 identity 模块：

```bash
git add zero-trust-identity
git commit -m "feat(identity): implement login endpoint"
```

如修改了 common：

```bash
git add zero-trust-common
git commit -m "chore(common): add token dto"
```

> **提交信息必须说明影响范围**

---

### Step 7：合并回 master

```bash
# 1. 切回 main 并同步
git checkout main
git pull origin main

# 2. 合并 feature 分支
git merge feature/identity-login

# 如有冲突：
#   - 手动解决
#   - git add 冲突文件
#   - git commit

# 3. 本地（远程）启动并自测自己模块

# 4. 推送 main
git push origin main

# 5. 删除本地分支
git branch -d feature/identity-login
```

---

## 二、项目整体架构与微服务说明

> 项目采用 **父 POM + 多模块微服务架构**，
> 通过统一依赖管理与微服务划分，保证系统结构一致、职责明确。

------

### 2.1 父 POM

**作用：**

- 统一管理全项目依赖版本
- 统一 Spring Boot / Spring Cloud 相关组件版本

### 2.2 微服务模块说明

------

#### `zero-trust-collecting`

**信息采集微服务**
 负责采集终端、行为、环境等原始数据，为系统提供基础数据输入。

------

#### `zero-trust-common`

**公共模块（非独立微服务）**
 用于存放各微服务共享的公共代码与统一定义，如返回结果、错误码、公共实体等。

------

#### `zero-trust-gateway`

**网关微服务**
 系统统一入口，负责请求转发、路由控制，并通过 Nacos 实现服务发现。

------

#### `zero-trust-monitoring`

**监控与响应微服务**
 负责对信息采集部分得到的数据进行汇总、数据清洗、处理。

------

#### `zero-trust-trusteval`

**动态信任评估微服务**
 根据采集数据与上下文信息，对主体的信任等级进行动态评估。

------

#### `zero-trust-policy`

**策略决策引擎微服务**
 根据当前信任状态与策略规则，进行访问控制与决策判断。

------

#### `zero-trust-identity`

**身份验证微服务**
 负责用户身份认证、身份信息管理及相关认证逻辑。

# 三、项目微服务已安装依赖说明

### **`spring-boot-starter-web`**

**提供 Web 与 REST API 能力，是微服务对外提供 HTTP 接口的基础依赖。**

------

### **`spring-boot-starter-test`**

**提供单元测试与 Spring 测试支持，仅在 test 阶段使用，不参与生产运行。**

------

### **`mysql-connector-j`**

**MySQL 官方 JDBC 驱动，用于微服务访问 MySQL 数据库。**

------

### **`spring-cloud-starter-alibaba-nacos-discovery`**

**用于微服务注册到 Nacos，实现服务注册与发现。**

------

### **`spring-cloud-starter-openfeign`**

**提供声明式 HTTP 客户端，用于微服务之间的通信接口调用。**

------

### **`spring-cloud-starter-loadbalancer`**

**提供客户端负载均衡能力，与 OpenFeign 配合使用。**

------

### **`zero-trust-common`**

**项目公共模块，包含通用 DTO、工具类、常量及统一约定内容。**

------

### **`lombok`**

**用于减少样板代码，通过注解自动生成常用方法，仅在编译期生效。**

### **`my-batis-plus`**

**用于简化 MyBatis 开发，通过强大的 CRUD 封装与代码生成器，提升数据访问层的开发效率。**

# **四、`zero-trust-common` 公共模块约定**

> `zero-trust-common` 为项目公共模块，用于承载**跨微服务共享的基础定义**，
> 所有微服务均已依赖该模块，保持返回结构、错误码、实体定义的一致性。

------

### 4.1 统一返回结果 `Result`

**包路径：**

```
zerotrust.common.Response
```

**说明：**

- 所有对前端的接口返回，**必须使用 `Result<T>` 进行封装**
- 禁止 Controller 直接返回实体类、Map 或基础类型

**统一约定：**

- `code`：业务状态码（200 表示成功，其它表示失败）
- `message`：提示信息
- `data`：实际返回数据

**使用方式示例：**

```
return Result.success(data);
return Result.success();
return Result.fail(ErrorCode.PARAM_ERROR);
```

------

### 4.2 统一错误码定义 `ErrorCode`

**包路径：**

```
zerotrust.common.Response
```

**说明：**

- 项目中所有业务错误码统一在 `ErrorCode` 枚举中定义
- 禁止在业务代码中硬编码数字错误码或错误信息

**约定：**

- 通用错误码由 common 统一维护
- 各模块如需新增错误码：
  - 在 `ErrorCode` 中新增
  - 命名需清晰表达业务含义
  - 不得随意修改已有错误码语义

------

### 4.3 公共实体类（Entity）统一存放

**包路径约定：**

```
zerotrust.common.Entity
```

**模块子目录划分：**

```
zerotrust.common.Entity
 ├── identity
 ├── policy
 ├── monitoring
 ├── collecting
 └── application
```

**说明：**

- 各微服务对外使用或跨模块共享的实体类，**统一放在 common 中**
- 每个业务模块在 `Entity` 下维护自己的子 package
- 禁止不同模块各自重复定义语义相同的实体类

# 五、微服务启动类、Feign 配置与配置文件约定（下述各微服务配置已在项目中配置完成）

> 本项目所有微服务在启动类、Feign 扫描、包扫描及配置文件结构上采用统一规范，
> 以保证服务发现、服务调用及配置管理的一致性。

------

### 5.1 微服务启动类统一约定

**所有微服务启动类必须满足以下要求：**

- 启用服务注册与发现
- 明确组件扫描路径
- 明确 Feign 客户端扫描路径
- 启动类位于各自模块的 `application` 包下

------

#### 示例：`zero-trust-monitoring` 启动类

```
@EnableDiscoveryClient
@ComponentScan(basePackages = "com.zerotrust")
@EnableFeignClients(value = "com.zerotrust.monitoring.feign")
@SpringBootApplication
public class MonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitoringApplication.class, args);
    }
}
```

**说明：**

- `@ComponentScan` 统一扫描 `com.zerotrust`
- `@EnableFeignClients` 只扫描当前模块的 feign 包

------

### 5.2 OpenFeign 包路径约定

**统一规则：**

- 每个微服务定义自己的 Feign 接口
- Feign 接口统一放在以下路径：

```
com.zerotrust.<module>.feign
```

**示例：**

- `com.zerotrust.monitoring.feign`
- `com.zerotrust.policy.feign`

❌ 禁止：

- Feign 接口散落在 service / controller 包中
- 扫描整个 `com.zerotrust` 作为 Feign Client

------

### 5.3 微服务配置文件基本约定（示例）

> 以下以业务微服务为例（非网关）

```
server:
  port: 9014

spring:
  application:
    name: zero-trust-trusteval

  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848

  datasource:
    url: jdbc:mysql://localhost:3306/ztrust_main?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC
    username: root
    password: 1231234
    driver-class-name: com.mysql.cj.jdbc.Driver
```

*说明：**

- `spring.application.name` 必须与服务注册名一致
- 端口号需在项目内统一规划，避免冲突
- 数据源配置按模块需要定义

------

### 5.4 网关服务特殊配置约定

#### 启动端口与应用类型

```
server:
  port: 8002
  address: 0.0.0.0

spring:
  main:
    web-application-type: reactive
  application:
    name: zero-trust-gateway
```

**约定：**

- 网关必须使用 `reactive`
- 网关作为系统唯一入口

------

#### Nacos 与负载均衡配置

```
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: public
        group: DEFAULT_GROUP

    loadbalancer:
      nacos:
        enabled: true
      ribbon:
        enabled: false
```

------

#### 网关路由配置（可后续自行修改）

```
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: false
      routes:
        - id: zero-trust-identity
          uri: lb://zero-trust-identity
          predicates:
            - Path=/identity/**

        - id: zero-trust-policy
          uri: lb://zero-trust-policy
          predicates:
            - Path=/policy/**

        - id: zero-trust-monitoring
          uri: lb://zero-trust-monitoring
          predicates:
            - Path=/monitoring/**

        - id: zero-trust-trusteval
          uri: lb://zero-trust-trusteval
          predicates:
            - Path=/trusteval/**

        - id: zero-trust-collecting
          uri: lb://zero-trust-collecting
          predicates:
            - Path=/collecting/**
```

**说明：**

- 路由统一在网关模块维护
- 后续新增或调整路由需统一修改此配置

### 

