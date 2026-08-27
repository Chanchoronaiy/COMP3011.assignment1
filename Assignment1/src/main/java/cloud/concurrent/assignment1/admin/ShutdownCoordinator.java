package cloud.concurrent.assignment1.admin;

/**
 * Defines the operations needed to coordinate the one-time transition from serving requests to
 * graceful shutdown.
 *
 * <p>An {@code interface} declares a contract without deciding how the work is performed. The
 * controller can depend on this contract while {@code SpringShutdownCoordinator} supplies the
 * real implementation. A test can provide a safer substitute that does not close the test JVM.</p>
 */
public interface ShutdownCoordinator {

    /**
     * Requests shutdown and returns {@code true} only for the first accepted request.
     * A {@code boolean} can contain only {@code true} or {@code false}.
     */
    boolean requestShutdown();

    /**
     * Returns whether this process has started its shutdown sequence.
     * Interface methods are implicitly public, so the {@code public} keyword is optional here.
     */
    boolean isShutdownRequested();
}
