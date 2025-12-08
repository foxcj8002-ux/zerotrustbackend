package zerotrust.common.Response;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),

    FAIL(500, "fail"),

    PARAM_ERROR(400, "Parameter error"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not found"),

    SERVER_ERROR(500, "Server internal error");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
