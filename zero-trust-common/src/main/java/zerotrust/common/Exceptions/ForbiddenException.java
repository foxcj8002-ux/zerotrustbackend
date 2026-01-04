package zerotrust.common.Exceptions;

import lombok.Getter;
import static zerotrust.common.Response.ErrorCode.FORBIDDEN;

/**
 * 已认证，但无权限（403）
 * 用户已登录
 * Token 合法
 * 但权限不足（角色/资源不允许）
 **/
@Getter
public class ForbiddenException extends CommonException {


    public ForbiddenException(String message) {
        super(FORBIDDEN.getCode(), message);
    }

    public ForbiddenException(int code, String message) {
        super(code, message);
    }

    public ForbiddenException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}