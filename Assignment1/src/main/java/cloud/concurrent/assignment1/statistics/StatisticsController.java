package cloud.concurrent.assignment1.statistics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Provides one process-wide view of transcription request statistics. */
@RestController // marks this class as an HTTP controller and serialises returned objects to JSON

@RequestMapping("/api/v1/global") // supplies the common URL prefix used by every endpoint in this controller
public class StatisticsController {

    private final RequestStatistics statistics; // final = dependency is assigned only once

    public StatisticsController(RequestStatistics statistics) {
        // !!! Constructor injection makes the dependency explicit and easy to replace in tests
        this.statistics = statistics;
    }

    // @GetMapping connects HTTP GET /api/v1/global/stats to this Java method
    @GetMapping("/stats")
    public GlobalStatsResponse getStatistics() {
        // Read immutable snapshot rather than exposing the live shared counter object
        StatisticsSnapshot snapshot = statistics.snapshot();
        return new GlobalStatsResponse(
                snapshot.totalInputTokens(),
                snapshot.totalOutputTokens());
    }

    /**
     * The API intentionally exposes token totals without internal performance measurements
     * The record component names become the JSON property names inputTokens and outputTokens
     */
    public record GlobalStatsResponse(long inputTokens, long outputTokens) {
    }
}
