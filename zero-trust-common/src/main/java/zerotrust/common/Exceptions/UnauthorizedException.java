package zerotrust.common.Exceptions;

import lombok.Getter;

import static zerotrust.common.Response.ErrorCode.UNAUTHORIZED;
/**
 * 未认证 / 未登录（401）
 * 未携带 Token
 * Token 失效
 * Token 不合法
 **/
@Getter
public class UnauthorizedException extends CommonException{

    public UnauthorizedException(String message) {
        super(UNAUTHORIZED.getCode(), message);
    }

    public UnauthorizedException(int code, String message) {
        super(code, message);
    }

    public UnauthorizedException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}