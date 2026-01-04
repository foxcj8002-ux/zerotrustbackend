package zerotrust.common.Response;

import lombok.Getter;

@Getter
public enum ErrorCode {

        /**
         * 成功
         */
        SUCCESS(200, "success"),

        // ================== 通用==================
        /**
         * 参数错误 / 请求不合法
         * 使用场景：
         * - 请求参数格式错误（类型不匹配、非法枚举值等）; JSON 反序列化失败; 缺少必填参数
         * 常见来源：
         * - 参数校验异常 ; BadRequestException
         * 建议前端：
         * - 提示用户修正输入
         */
        PARAM_ERROR(400, "Parameter error"),

        /**
         * 未认证 / 未登录
         * 使用场景：
         * - 请求未携带 Token; Token 失效或非法
         * 常见来源：
         * - 鉴权过滤器;  Gateway / Security 层
         * 建议前端：
         * - 跳转登录页或重新登录
         */
        UNAUTHORIZED(401, "Unauthorized"),

        /**
         * 已认证但无权限
         * 使用场景：
         * - 用户已登录，但没有访问该资源的权限; 角色 / 权限校验不通过
         * 常见来源：
         * - 权限拦截器ForbiddenException
         * 建议前端：
         * - 提示“无权限访问”
         */
        FORBIDDEN(403, "Forbidden"),

        /**
         * 资源不存在
         * 使用场景：
         * - 请求的资源不存在; 访问了不存在的接口路径
         * 常见来源：
         * - 业务服务主动抛出; Gateway 找不到下游接口（非路由问题）
         * 建议前端：
         * - 展示 404 页面或友好提示
         */
        NOT_FOUND(404, "Not found"),

        // ================== 网关 & 下游服务 ==================
        /**
         * 网关未找到匹配路由
         * 使用场景：
         * - 请求路径未命中任何 Gateway 路由; 路由配置错误 ; 下游服务未注册到 Nacos
         * 常见来源：
         * - Spring Cloud Gateway
         * 建议前端：
         * - 提示“服务不可用”
         */
        GATEWAY_NO_ROUTE(404, "No route found"),

        /**
         * 下游服务不可用
         *
         * 使用场景：
         * - 下游服务宕机; 无可用实例（负载均衡选不到节点）; 网络连接失败
         * 常见来源：
         * - ConnectException; UnknownHostException; 服务未注册或全部实例下线
         * 建议前端：
         * - 提示稍后重试
         */
        SERVICE_UNAVAILABLE(503, "Service unavailable"),

        /**
         * 网关 / 下游服务调用超时
         *
         * 使用场景：
         * - 网关调用下游接口超时; 下游服务响应时间过长
         * 常见来源：
         * - TimeoutException ;ReadTimeoutException
         *
         * 建议前端：
         * - 提示“请求超时，请稍后重试”
         */
        GATEWAY_TIMEOUT(504, "Gateway timeout"),

        /**
         * 请求过于频繁（限流）
         *
         * 使用场景：
         * - Gateway 限流规则触发; Sentinel / Redis RateLimiter 拒绝请求
         * 常见来源：
         * - 限流过滤器; ResponseStatusException(429)
         * 建议前端：
         * - 提示“请求过于频繁，请稍后再试”
         */
        TOO_MANY_REQUESTS(429, "Too many requests"),

        // ================== 系统级异常 ==================
        /**
         * 通用失败（兜底）
         *
         * 使用场景：
         * - 未分类的系统异常; Exception 兜底处理
         * 常见来源：
         * - 未捕获异常
         */
        FAIL(500, "fail"),

        /**
         * 服务内部错误
         *
         * 使用场景：
         * - 服务自身代码异常; 数据库/中间件异常; 序列化/反序列化异常
         * 常见来源：
         * - DbException; 系统运行时异常
         */
        SERVER_ERROR(500, "Server internal error");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
