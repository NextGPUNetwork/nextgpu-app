package ai.nextgpu.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode{
    // Custom application error codes (1000+ series)
    // Validation & Constraint Errors

    UNKNOWN_ERROR(500, HttpStatus.Series.SERVER_ERROR, "Unknown Error"),
    AUTHENTICATION_FAILED(1000, HttpStatus.Series.CLIENT_ERROR, "Authentication failed"),
    ACCESS_DENIED(1005, HttpStatus.Series.CLIENT_ERROR, "Access denied"),
    VALIDATION_FAILED(1001, HttpStatus.Series.CLIENT_ERROR, "Validation failed"),
    CONSTRAINT_VIOLATION(1002, HttpStatus.Series.CLIENT_ERROR, "Constraint violation"),
    INVALID_OTP(1003, HttpStatus.Series.CLIENT_ERROR, "Invalid OTP provided!"),
    INVALID_INPUT(1004, HttpStatus.Series.CLIENT_ERROR, "Invalid input"),

    // Business Rule & Comparison Errors
    COMPARISON_FAILED(1100, HttpStatus.Series.CLIENT_ERROR, "Comparison failed"),
    THRESHOLD_EXCEEDED(1101, HttpStatus.Series.CLIENT_ERROR, "Threshold exceeded"),
    MAX_TOLERANCE_VIOLATION(1102, HttpStatus.Series.CLIENT_ERROR, "Maximum allowed tolerance violation"),
    MINIMUM_REQUIREMENT_NOT_MET(1103, HttpStatus.Series.CLIENT_ERROR, "Minimum requirement not met"),
    UNSUPPORTED_SPECIFICATION(1104, HttpStatus.Series.CLIENT_ERROR, "Unsupported specification"),
    INCOMPATIBLE_VALUES(1105, HttpStatus.Series.CLIENT_ERROR, "Incompatible values"),

    // Resource & Configuration Errors
    INSUFFICIENT_RESOURCE(1200, HttpStatus.Series.CLIENT_ERROR, "Insufficient resource"),
    CONFIGURATION_ERROR(1201, HttpStatus.Series.SERVER_ERROR, "Configuration error"),
    RESOURCE_UNAVAILABLE(1202, HttpStatus.Series.SERVER_ERROR, "Resource unavailable"),

    // Processing & Service Errors
    PROCESSING_ERROR(1300, HttpStatus.Series.SERVER_ERROR, "Processing error"),
    AUDIT_FAILED(1301, HttpStatus.Series.SERVER_ERROR, "Audit failed"),
    CALCULATION_ERROR(1302, HttpStatus.Series.SERVER_ERROR, "Calculation error"),

    // Data & Integrity Errors
    DATA_INTEGRITY_VIOLATION(1400, HttpStatus.Series.CLIENT_ERROR, "Data integrity violation"),
    DATA_INCONSISTENCY(1401, HttpStatus.Series.CLIENT_ERROR, "Data inconsistency"),

    // Domain-Specific Hardware Errors
    HARDWARE_SPECIFICATION_ERROR(1500, HttpStatus.Series.CLIENT_ERROR, "Hardware specification error"),
    PERFORMANCE_THRESHOLD_EXCEEDED(1501, HttpStatus.Series.CLIENT_ERROR, "Performance threshold exceeded"),
    COMPATIBILITY_ISSUE(1502, HttpStatus.Series.CLIENT_ERROR, "Compatibility issue"),
    SPECIFICATION_MISMATCH_ERROR(1503, HttpStatus.Series.CLIENT_ERROR, "Required Specification Mismatched"),
    BENCHMARK_SCORE_DEVIATION(1504, HttpStatus.Series.CLIENT_ERROR, "Benchmark score deviation exceeds acceptable range"),
    BENCHMARK_REFERENCE_MISMATCH(1505, HttpStatus.Series.CLIENT_ERROR, "Reference benchmark data mismatch"),
    BENCHMARK_INCOMPLETE(1506, HttpStatus.Series.CLIENT_ERROR, "Benchmark results incomplete"),
    BENCHMARK_ENVIRONMENT_INVALID(1507, HttpStatus.Series.CLIENT_ERROR, "Invalid benchmark environment"),
    BENCHMARK_RESULTS_NOT_FOUND(1508, HttpStatus.Series.CLIENT_ERROR, "Benchmark results not found"),
    COMPONENT_NOT_FOUND(1600, HttpStatus.Series.CLIENT_ERROR, "Component not found"),
    RESOURCE_NOT_FOUND(1601, HttpStatus.Series.CLIENT_ERROR, "Resource not found"),
    AI_SERVICE_ERROR(1700, HttpStatus.Series.SERVER_ERROR, "AI service error"),
    AI_SERVICE_DISABLED(1701, HttpStatus.Series.SERVER_ERROR, "AI service disabled"),
    DATA_PERSISTENCE_ERROR(1800, HttpStatus.Series.SERVER_ERROR, "Data persistence error"),
    InsufficientStakedBalance(1900, HttpStatus.Series.CLIENT_ERROR, "Insufficient Staked balance, required to become a Provider error");

    private final int code;
    private final HttpStatus.Series series;
    private final String description;

    public boolean isClientError() {
        return this.series == HttpStatus.Series.CLIENT_ERROR;
    }

    public boolean isServerError() {
        return this.series == HttpStatus.Series.SERVER_ERROR;
    }

    /**
     * Convert custom error code to appropriate HTTP status
     * Used by exception handlers to map to HTTP response
     */
    public HttpStatus toHttpStatus() {
        if (isClientError()) {
            return HttpStatus.BAD_REQUEST; // or more specific based on your needs
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException("No ErrorCode found for code: " + code);
    }
}
