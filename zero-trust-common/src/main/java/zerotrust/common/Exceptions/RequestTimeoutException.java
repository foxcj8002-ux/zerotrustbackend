package zerotrust.common.Exceptions;



/**
 * 请求超时异常
 * 调用下游服务超时
 * 外部接口响应慢
 * 任务执行超时
 **/
public class RequestTimeoutException  extends CommonException {
    public RequestTimeoutException(String message) {
        super(message);
    }

    public RequestTimeoutException(int code, String message) {
        super(code, message);
    }

    public RequestTimeoutException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}