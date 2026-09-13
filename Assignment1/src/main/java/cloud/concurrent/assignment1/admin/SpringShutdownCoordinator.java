package cloud.concurrent.assignment1.admin;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/** Uses Spring's lifecycle support to stop the embedded server and exit with code zero. */
// @Component makes this class a Spring-managed object (aka a bean).
// Spring can therefore inject it wherever the ShutdownCoordinator interface is required
@Component
// implements = this class supplies every method declared by the interface
public class SpringShutdownCoordinator implements ShutdownCoordinator {

    // static = one logger is shared by every object of this class
    // final = LOGGER reference cannot be reassigned after it has been initialised
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringShutdownCoordinator.class);
    private static final Duration RESPONSE_FLUSH_DELAY = Duration.ofMillis(250);

    // THIS IS MADE WITH THE HELP OF CHATGPT
    // compareAndSet below changes false to true as one atomic operation. prevents two
    // request threads from both believing that they initiated the first shutdown.
    private final AtomicBoolean shutdownRequested = new AtomicBoolean();
    private final ConfigurableApplicationContext applicationContext;

    // Constructor injection gives this component access to the running Spring application
    public SpringShutdownCoordinator(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    // @Override confirms that this method implements one declared by ShutdownCoordinator
    @Override
    public boolean requestShutdown() {
        // An atomic compare-and-set = thread-safe equivalent of:
        // "if false, change to true", without a race between the check and the change (week 2 lecture 3)
        if (!shutdownRequested.compareAndSet(false, true)) {
            return false;
        }

        // A separate platform thread lets the controller finish writing its HTTP 202 response.
        // ofPlatform() builds a traditional operating-system-backed Java thread.
        Thread.ofPlatform()
                .name("application-shutdown")
                // this::shutDownAfterResponse is a method reference. It supplies the work
                // that the new thread will run, like a compact Runnable implementation.
                .start(this::shutDownAfterResponse);
        return true;
    }

    @Override
    public boolean isShutdownRequested() {
        return shutdownRequested.get();
    }

    private void shutDownAfterResponse() {
        try {
            // wait a bit so the HTTP response can leave the server before its context closes
            Thread.sleep(RESPONSE_FLUSH_DELAY);
        } catch (InterruptedException exception) {
            // sleep may throw InterruptedException when another thread asks this one to stop waiting
            // Restore the interrupt flag instead of silently losing another thread's signal
            Thread.currentThread().interrupt();
        }

        LOGGER.info("Graceful shutdown requested through the administration API");
        // Closing the Spring context runs its normal lifecycle hooks and drains current work
        // () -> 0 is a lambda that supplies the successful process exit code
        int exitCode = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(exitCode);
    }
}
