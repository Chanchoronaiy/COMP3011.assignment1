package cloud.concurrent.assignment1.transcription;

/**
 * Raised when the server was started without its required OpenAI API key.
 *
 * <p>The message deliberately describes only the configuration problem. It
 * never includes the secret key or other sensitive server information.</p>
 */
// This custom RuntimeException is unchecked, so it can travel to the shared
// API error handler without adding throws declarations to every calling method.
public class MissingApiKeyException extends RuntimeException {

    // This is a no-argument constructor because callers do not need to choose a message.
    public MissingApiKeyException() {
        // The parent RuntimeException stores this fixed, safe message.
        super("The transcription service is not configured on this server.");
    }
}
