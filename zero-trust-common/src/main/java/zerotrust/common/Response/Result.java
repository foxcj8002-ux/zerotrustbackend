package zerotrust.common.Response;


import lombok.Data;

/**
 * 统一返回结果
 */
@Data
public class Result<T> {
    /**
     * 业务状态码（200成功，其它失败）
     */
    private Integer code;
    /**
     * 提示信息
     */
    private String message;
    /**
     * 返回数据
     */
    private T data;
    /**
     * 成功，无返回数据
     */
    public static Result<Void> success() {
        Result<Void> r = new Result<>();
        r.setCode(ErrorCode.SUCCESS.getCode());
        r.setMessage(ErrorCode.SUCCESS.getMessage());
        return r;
    }
    /**
     * 成功，带数据
     */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(ErrorCode.SUCCESS.getCode());
        r.setMessage(ErrorCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    /**
     * 失败（自定义消息）
     */
    public static Result<Void> fail(String message) {
        Result<Void> r = new Result<>();
        r.setCode(ErrorCode.FAIL.getCode());
        r.setMessage(message);
        return r;
    }

    /**
     * 失败（使用标准错误码）
     */
    public static Result<Void> fail(ErrorCode errorCode) {
        Result<Void> r = new Result<>();
        r.setCode(errorCode.getCode());
        r.setMessage(errorCode.getMessage());
        return r;
    }

    public static Result<Void> fail(int code, String message) {
        Result<Void> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
