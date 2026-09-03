package cloud.concurrent.assignment1.statistics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Verifies that internal measurements do not leak into the prescribed statistics response. */
class StatisticsControllerTests {

    // @Test marks this method as a test that JUnit should run automatically.
    @Test
    // MockMvc methods can report checked exceptions, so the test passes them to JUnit.
    void responseContainsExactlyTheTwoTokenCounters() throws Exception {
        // Arrange: create known statistics and the controller being tested.
        RequestStatistics statistics = new RequestStatistics();
        statistics.recordStarted(500);
        statistics.recordSucceeded(14, 6, 1_000_000);

        // MockMvc exercises the HTTP controller without starting a real web server.
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new StatisticsController(statistics))
                .build();

        // Act and assert: request the endpoint, then check its status and JSON fields.
        mockMvc.perform(get("/api/v1/global/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputTokens").value(14))
                .andExpect(jsonPath("$.outputTokens").value(6))
                .andExpect(jsonPath("$.length()").value(2));
    }
}
