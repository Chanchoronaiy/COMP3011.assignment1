package cloud.concurrent.assignment1.admin;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cloud.concurrent.assignment1.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Regression tests for the exact administration API contract supplied with the assignment. */
class AdministrationControllerTests {

    @Test
    void uptimeContainsBothUtcTimestampsAndElapsedSeconds() throws Exception {
        // standaloneSetup tests this controller without launching a complete server.
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new UptimeController())
                .build();

        // The chained andExpect calls check the HTTP status and exact JSON contract.
        mockMvc.perform(get("/api/v1/admin/uptime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utcServerStart").isString())
                .andExpect(jsonPath("$.utcNow").isString())
                .andExpect(jsonPath("$.serverUptimeSeconds", greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void firstShutdownIsAcceptedAndSecondShutdownReturnsConflict() throws Exception {
        // Use a harmless test double so this test never shuts down the real test process.
        StubShutdownCoordinator coordinator = new StubShutdownCoordinator();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ShutdownController(coordinator))
                // Controller advice converts the duplicate request exception into HTTP 409.
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        // The first request must be accepted because shutdown has not started yet.
        mockMvc.perform(post("/api/v1/admin/shutdown"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Graceful shutdown requested."))
                .andExpect(jsonPath("$.length()").value(1));

        // Repeating the request must return Conflict instead of starting shutdown twice.
        mockMvc.perform(post("/api/v1/admin/shutdown"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Graceful shutdown is already in progress."))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/shutdown"));
    }

    /** Avoids closing the real test JVM while preserving the coordinator's state transition. */
    private static final class StubShutdownCoordinator implements ShutdownCoordinator {

        private boolean shutdownRequested;

        @Override
        public boolean requestShutdown() {
            // The boolean stores state between the first and second simulated requests.
            if (shutdownRequested) {
                return false;
            }

            shutdownRequested = true;
            return true;
        }

        @Override
        public boolean isShutdownRequested() {
            return shutdownRequested;
        }
    }
}
