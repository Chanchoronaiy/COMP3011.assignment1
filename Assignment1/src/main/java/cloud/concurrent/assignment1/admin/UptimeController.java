package cloud.concurrent.assignment1.admin;

import java.time.Duration;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reports how long this Java process has been serving requests. */
// Spring creates one UptimeController object and uses it to handle HTTP requests.
@RestController
@RequestMapping("/api/v1/admin")
public class UptimeController {

    // Instant represents one exact moment on the UTC timeline.
    // This field is initialised when Spring creates the controller. final prevents
    // it from being reassigned, so every request uses the same server start time.
    private final Instant utcServerStart = Instant.now();

    // @GetMapping connects HTTP GET /api/v1/admin/uptime to this method.
    @GetMapping("/uptime")
    public UptimeResponse getUptime() {
        // This is a local variable: each request receives its own current timestamp.
        Instant utcNow = Instant.now();

        // The supplied API contract defines uptime as the difference between these UTC values.
        // Duration.between calculates the elapsed time without changing either Instant.
        // Dividing nanoseconds by 1,000,000,000.0 converts them to decimal seconds.
        // The .0 makes this floating-point division instead of whole-number division.
        double uptimeSeconds = Duration.between(utcServerStart, utcNow).toNanos()
                / 1_000_000_000.0;

        // new creates a response record containing the three calculated values.
        // Because this is a @RestController, Spring converts the record into JSON.
        return new UptimeResponse(utcServerStart, utcNow, uptimeSeconds);
    }

    /**
     * All three property names are prescribed by assignment1api.yaml.
     * Java automatically creates the record's constructor and accessor methods.
     */
    public record UptimeResponse(
            Instant utcServerStart,
            Instant utcNow,
            double serverUptimeSeconds) {
    }
}
