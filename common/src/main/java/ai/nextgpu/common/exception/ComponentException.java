package ai.nextgpu.common.exception;

public class ComponentException extends BaseException {

    public ComponentException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ComponentException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }

    public ComponentException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }

    public ComponentException (ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ComponentException(String message, boolean enableSuppression, boolean writableStackTrace, ErrorCode errorCode, Throwable cause) {
        super(message, enableSuppression, writableStackTrace, errorCode, cause);
    }
}
