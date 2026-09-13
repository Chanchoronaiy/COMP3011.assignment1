package cloud.concurrent.assignment1.admin;

/**
 * Raised when a second shutdown request arrives after shutdown has started.
 *
 * {@code extends RuntimeException} = this class inherits the normal
 * behaviour of Java's {@link RuntimeException}.
 *  A runtime exception is"unchecked" so methods that may raise it do not need to add a{@code throws} declaration.
 *
 * Using a separate exception class makes the problem easy to identify. The
 * API error handler can later recognise this exact type and return HTTP 409
 * Conflict response instead of exposing internal application details.
 */
public class ShutdownInProgressException extends RuntimeException {

    //contructor
    public ShutdownInProgressException() {
        // super(...) calls RuntimeException's constructor and stores this message.
        super("Graceful shutdown is already in progress.");
    }
}
