package cloud.concurrent.assignment1.transcription;

/**
 * Raised when the remote transcription provider cannot complete a request.
 *
 * The application can log the original technical cause for debugging while
 * the API handler returns a separate, safe message to the browser
 */
// A RuntimeException is unchecked, so service methods can raise this failure
// w/out adding a throws declaration to the TranscriptionService interface
public class TranscriptionServiceException extends RuntimeException {

    public TranscriptionServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
