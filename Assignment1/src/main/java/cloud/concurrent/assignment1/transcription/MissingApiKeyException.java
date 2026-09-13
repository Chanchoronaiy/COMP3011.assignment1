package cloud.concurrent.assignment1.transcription;

/**
 * Raised when the server was started without its required OpenAI API key.
 *
 * The message deliberately describes only the configuration problem. It
 * never includes the secret key or other sensitive server information.
 */
// This custom RuntimeException is unchecked, so it can travel to the shared
// API error handler w/out adding throws declarations to every calling method
public class MissingApiKeyException extends RuntimeException {

    // no-argument constructor = callers do not need to choose a message.
    public MissingApiKeyException() {
        super("The transcription service is not configured on this server.");
    }
}
