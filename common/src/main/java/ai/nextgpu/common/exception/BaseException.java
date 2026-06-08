package ai.nextgpu.common.exception;

public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;


    public BaseException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public BaseException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BaseException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public BaseException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    public BaseException(String message, boolean enableSuppression, boolean writableStackTrace, ErrorCode errorCode, Throwable cause) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }
}
