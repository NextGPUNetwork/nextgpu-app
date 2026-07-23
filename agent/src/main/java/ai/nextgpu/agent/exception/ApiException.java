package ai.nextgpu.agent.exception;

import ai.nextgpu.common.dto.ErrorResponseDto;
import ai.nextgpu.common.exception.BaseException;
import lombok.Getter;

@Getter
public class ApiException extends BaseException {
    public final ErrorResponseDto errorResponse;

    public ApiException(ErrorResponseDto errorResponse) {
        super(errorResponse.getErrorCode());
        this.errorResponse = errorResponse;
    }

    public ApiException(ErrorResponseDto errorResponse, Throwable cause) {
        super(errorResponse.getMessage(), errorResponse.getErrorCode(), cause);
        this.errorResponse = null;
    }
}
