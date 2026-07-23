package ai.nextgpu.agent.exception;

/**
 * Raised whenever text-to-image generation cannot complete. Always carries a
 * specific, human-readable reason and (when available) the underlying cause,
 * and is always logged at the point it's thrown so failures are never silent.
 */
public class VisionGenerationException extends RuntimeException {
    public VisionGenerationException(String message) {
        super(message);
    }

    public VisionGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
