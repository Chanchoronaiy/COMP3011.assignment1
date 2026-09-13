package cloud.concurrent.assignment1.transcription;

/**
 * Raised when uploaded data is empty, too large, or not a supported audio type.
 *
 * This exception type lets the shared API error handler distinguish a
 * bad recording from an internal server failure. returns a safe client error.</p>
 */

// RuntimeException is unchecked, so callers do not need a throws declaration.
public class InvalidRecordingException extends RuntimeException {

    public InvalidRecordingException(String message) {     // parameter for describing validation failure
        // super(message) passes it to RuntimeException, where getMessage() can retrieve it
        super(message);
    }
}
