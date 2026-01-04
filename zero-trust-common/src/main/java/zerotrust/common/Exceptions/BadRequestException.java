package zerotrust.common.Exceptions;

import lombok.Getter;

import static zerotrust.common.Response.ErrorCode.PARAM_ERROR;

/**
 * 请求不合法(400)
 * 参数格式错误（但不是业务规则错误）
 * JSON 结构错误
 * 缺少必要字段
 * 枚举值不合法
 **/
@Getter
public class BadRequestException extends CommonException{

    public BadRequestException(String message) {
        super(PARAM_ERROR.getCode(), message);
    }

    public BadRequestException(int code, String message) {
        super(code, message);
    }

    public BadRequestException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}