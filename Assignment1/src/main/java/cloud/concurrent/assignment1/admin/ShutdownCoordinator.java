package cloud.concurrent.assignment1.admin;

/**
 * Defines the operations needed to coordinate the one-time transition from serving requests to
 * graceful shutdown.
 *
 * An {@code interface} declares a contract without deciding how the work is performed. The
 * controller can depend on this contract while {@code SpringShutdownCoordinator} supplies the
 * real implementation. A test can provide a safer substitute that does not close the test JVM.
 */
public interface ShutdownCoordinator {

    boolean requestShutdown();

    /**
     * Returns whether this process has started its shutdown sequence.
     */
    boolean isShutdownRequested();
}
