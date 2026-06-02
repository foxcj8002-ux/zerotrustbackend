# Zero-Trust Gateway Playwright Tests

Playwright E2E 测试，用于测试 zero-trust-rgw 网关的零信任访问控制功能。

## 功能覆盖

- **认证测试**: JWT Token 验证、Cookie 处理
- **策略校验**: Redis 策略验证、用户/资源 ID 匹配
- **SSRF 防护**: 内网 IP 拦截、白名单验证
- **透明代理**: 请求转发、子路径处理、Query 参数传递SS
- **内部端点**: 健康检查、策略接收

## 前置条件

### 1. 环境依赖

确保以下服务正在运行：

- **Redis**: `127.0.0.1:6379` - 存储访问策略
- **MySQL**: `127.0.0.1:3306/zerotrust_rgw` - 存储资源映射
- **Nacos**: `127.0.0.1:8848` - 配置中心
- **Casdoor 证书**: `/etc/gateway/certs/casdoor-cert.pem`

### 2. 测试数据

执行 SQL 脚本初始化测试资源：

```bash
mysql -h 127.0.0.1 -u root -p zerotrust_rgw < src/test/resources/test-data.sql
```

### 3. Casdoor 私钥

将 Casdoor 私钥文件复制到测试目录：

```bash
cp /path/to/casdoor-key.pem src/test/playwright/utils/casdoor-key.pem
```

## 快速开始

### 方式一: 使用 Maven (推荐)

```bash
cd zero-trust-rgw

# 安装依赖并运行测试
mvn test -DskipTests=false -pl zero-trust-rgw

# 仅运行 Playwright 测试
mvn frontend: npm@playwright-test
```

### 方式二: 手动运行

```bash
cd src/test/playwright

# 安装依赖
npm install

# 安装浏览器
npx playwright install chromium --with-deps

# 运行测试
npm test

# 查看报告
npx playwright show-report
```

## 测试配置

### 环境变量

| 变量 | 默认值 | 说明 |
|-----|-------|------|
| `GATEWAY_URL` | `https://localhost:9527` | 网关地址 |
| `CI` | `false` | CI 模式 (重试、串行) |

### 修改 baseURL

```bash
export GATEWAY_URL=https://your-gateway-host:9527
npm test
```

## 测试用例

### TC01-03: 认证测试
- TC01: 缺少 Token
- TC02: 无效 Token
- TC03: 过期 Token

### TC04-07: 策略校验
- TC04: 策略未下发
- TC05: 用户 ID 不匹配
- TC06: 资源 ID 不匹配
- TC07: 策略过期

### TC08-11: SSRF 防护
- TC08: 127.0.0.1 拦截
- TC09: 10.x.x.x 拦截
- TC10: 192.168.x.x 拦截
- TC11: 169.254.x.x 拦截

### TC12-15: 透明代理
- TC12: 百度首页代理
- TC13: JSONPlaceholder API 代理
- TC14: 子路径转发
- TC15: Query 参数转发

### TC16-18: 内部端点
- TC16: 健康检查
- TC17: 策略接收
- TC18: 策略校验失败

## 调试

### 有头模式
```bash
npm run test:headed
```

### 调试模式
```bash
npm run test:debug
```

### 只运行特定测试
```bash
npx playwright test --grep "TC01"
```

### 查看 Trace
Playwright 会在测试失败时自动记录 trace，使用以下命令查看：

```bash
npx playwright show-trace trace.zip
```

## 报告

测试完成后，报告位于 `playwright-report/index.html`

## 故障排除

### 1. 证书错误
如果遇到 HTTPS 证书错误，确保 `ignoreHTTPSErrors: true` 在 `playwright.config.ts` 中设置。

### 2. 连接超时
检查网关是否正常运行：
```bash
curl -k https://localhost:9527/gateway/policy/health
```

### 3. Redis 连接失败
```bash
redis-cli ping
```

### 4. MySQL 连接失败
```bash
mysql -h 127.0.0.1 -u root -p -e "SELECT 1"
```
