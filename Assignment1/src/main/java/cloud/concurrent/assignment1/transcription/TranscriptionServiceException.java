package cloud.concurrent.assignment1.transcription;

/**
 * Raised when the remote transcription provider cannot complete a request.
 *
 * <p>The application can log the original technical cause for debugging while
 * the API handler returns a separate, safe message to the browser.</p>
 */
// A RuntimeException is unchecked, so service methods can raise this failure
// without adding a throws declaration to the TranscriptionService interface.
public class TranscriptionServiceException extends RuntimeException {

    // Throwable is the common parent type for Java exceptions and errors.
    // Passing the original cause preserves the chain of failures for logs and tests.
    public TranscriptionServiceException(String message, Throwable cause) {
        // This parent constructor stores both our explanation and the underlying cause.
        super(message, cause);
    }
}
