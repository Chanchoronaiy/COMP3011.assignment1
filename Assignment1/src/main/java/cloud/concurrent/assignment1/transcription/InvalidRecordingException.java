package cloud.concurrent.assignment1.transcription;

/**
 * Raised when uploaded data is empty, too large, or not a supported audio type.
 *
 * <p>A specific exception type lets the shared API error handler distinguish a
 * bad recording from an internal server failure and return a safe client error.</p>
 */
// extends creates an inheritance relationship with RuntimeException.
// RuntimeException is unchecked, so callers do not need a throws declaration.
public class InvalidRecordingException extends RuntimeException {

    // message is a constructor parameter describing the particular validation failure.
    public InvalidRecordingException(String message) {
        // super(message) passes it to RuntimeException, where getMessage() can retrieve it.
        super(message);
    }
}
