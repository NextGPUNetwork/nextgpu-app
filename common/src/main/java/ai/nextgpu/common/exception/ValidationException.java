package ai.nextgpu.common.exception;


public class ValidationException extends BaseException {


    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ValidationException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }

    public ValidationException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }

    public ValidationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ValidationException(String message, boolean enableSuppression, boolean writableStackTrace, ErrorCode errorCode, Throwable cause) {
        super(message, enableSuppression, writableStackTrace, errorCode, cause);
    }
}
