package cloud.concurrent.assignment1.transcription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cloud.concurrent.assignment1.config.OpenAiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Verifies the cloud API boundary without making an external network request. */
class OpenAiTranscriptionServiceTests {

    @Test
    void missingApiKeyFailsBeforeAnyNetworkRequest() {
        // Arrange with an empty API key to represent a server that was not configured.
        OpenAiProperties properties = new OpenAiProperties(
                "",
                "gpt-4o-mini-transcribe",
                "https://api.openai.test");
        OpenAiTranscriptionService service = new OpenAiTranscriptionService(
                RestClient.builder().baseUrl(properties.baseUrl()).build(),
                properties);

        // assertThatThrownBy verifies both the exception type and its safe user message.
        assertThatThrownBy(() -> service.transcribe(new AudioRecording(
                        new byte[] {1},
                        "audio/webm",
                        "recording.webm")))
                .isInstanceOf(MissingApiKeyException.class)
                .hasMessage("The transcription service is not configured on this server.");
    }

    @Test
    void mapsOpenAiTextAndTokenUsage() {
        // The builder creates the same RestClient shape used by the real application.
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.openai.test")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-key");
        // This mock server intercepts the request in memory, so no paid API call is made.
        MockRestServiceServer mockServer = MockRestServiceServer
                .bindTo(builder)
                .build();
        OpenAiProperties properties = new OpenAiProperties(
                "test-key",
                "gpt-4o-mini-transcribe",
                "https://api.openai.test");
        OpenAiTranscriptionService service =
                new OpenAiTranscriptionService(builder.build(), properties);

        // Describe the exact outgoing request and the fake JSON response it should receive.
        mockServer.expect(once(), requestTo("https://api.openai.test/v1/audio/transcriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(content().string(containsString("gpt-4o-mini-transcribe")))
                .andRespond(withSuccess(
                        // Three quotation marks form a Java text block for multi-line text.
                        """
                        {
                          "text": "hello from OpenAI",
                          "usage": {
                            "input_tokens": 14,
                            "output_tokens": 5,
                            "total_tokens": 19
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        // Act: call the service with a small pretend WebM recording.
        TranscriptionResult result = service.transcribe(new AudioRecording(
                new byte[] {1, 2, 3},
                "audio/webm",
                "recording.webm"));

        // Assert: the service converted the JSON into the application's result model.
        assertThat(result.text()).isEqualTo("hello from OpenAI");
        assertThat(result.inputTokens()).isEqualTo(14);
        assertThat(result.outputTokens()).isEqualTo(5);
        // verify() confirms that the expected HTTP request occurred exactly once.
        mockServer.verify();
    }
}
