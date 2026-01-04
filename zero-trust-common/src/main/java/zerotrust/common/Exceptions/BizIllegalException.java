package zerotrust.common.Exceptions;

/**
 * 业务非法 / 业务规则不允许
 * 订单已完成，不能取消
 * 用户状态不允许执行该操作
 * 当前流程不允许跳转
 **/
public class BizIllegalException extends CommonException{
    public BizIllegalException(String message) {
        super(message);
    }

    public BizIllegalException(int code, String message) {
        super(code, message);
    }

    public BizIllegalException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}