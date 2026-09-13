package cloud.concurrent.assignment1.admin;

import java.time.Duration;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reports how long this Java process has been serving requests. */
// uses it to handle HTTP requests
@RestController
@RequestMapping("/api/v1/admin")
public class UptimeController {

    // INSTANT represents one exact moment on the UTC timelinE. initialised when Spring creates the controller. 
    // final prevents it from being reassigned, so every request uses the same server start time.
    private final Instant utcServerStart = Instant.now();

    // connects HTTP GET /api/v1/admin/uptime to this method.
    @GetMapping("/uptime")
    public UptimeResponse getUptime() {
        Instant utcNow = Instant.now(); //each request receives its own current timestamp.

        // uptime = difference between these UTC values.
        // Duration.between = calculates elapsed time w/OUT changing either Instant
        // DIVIDING nanoseconds by 1,000,000,000.0 converts into seconds
        double uptimeSeconds = Duration.between(utcServerStart, utcNow).toNanos() / 1_000_000_000.0;

        return new UptimeResponse(utcServerStart, utcNow, uptimeSeconds); // BC this is a @RestController, Spring converts the record into JSON.
    }

    /**
     * names prescribed by assignment1api.yaml. Java auto creates the record's constructor and accessor methods.
     */
    public record UptimeResponse(
            Instant utcServerStart,
            Instant utcNow,
            double serverUptimeSeconds) {
    }
}
