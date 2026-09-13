package cloud.concurrent.assignment1.transcription;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import cloud.concurrent.assignment1.config.OpenAiProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Sends recordings to OpenAI's speech-to-text endpoint. */
// @Service registers this implementation as a Spring-managed service object.
@Service
// use this real service unless the local-stub profile is active
@Profile("!local-stub")
// implements connects this class to the provider-independent service contract
public class OpenAiTranscriptionService implements TranscriptionService {

    private final RestClient openAiRestClient;
    private final OpenAiProperties properties;

    // constructor injection supplies reusable HTTP client and external configuration
    public OpenAiTranscriptionService(RestClient openAiRestClient, OpenAiProperties properties) {
        this.openAiRestClient = openAiRestClient;
        this.properties = properties;
    }

    @Override
    public TranscriptionResult transcribe(AudioRecording recording) {
        // fail on the server before making a request if TITAN did not provide the environment key
        if (!properties.hasApiKey()) {
            throw new MissingApiKeyException();
        }

        // OpenAI expects multipart/form-data: one part names the model and one contains the file
        // MultiValueMap<String, Object>: keys are Strings while each value can be a String, file resource, or another suitable Object
        MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
        multipartBody.add("model", properties.model());

        // headers belong only to the audio part inside the multipart request
        HttpHeaders audioHeaders = new HttpHeaders();
        audioHeaders.setContentType(MediaType.parseMediaType(recording.contentType()));
        multipartBody.add(
                "file",
                // HttpEntity groups the file data with that part's HTTP headers
                new HttpEntity<>(
                        new NamedByteArrayResource(recording.bytes(), recording.fileName()), audioHeaders));

        try {
            // retrieve() sends the blocking HTTP request; body(...) converts JSON into records
            // fluent chain configures one request step by step and then executes it
            OpenAiResponse response = openAiRestClient
                    .post()
                    .uri("/v1/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipartBody)
                    .retrieve()
                    .body(OpenAiResponse.class);

            if (response == null || response.text() == null) {
                throw new TranscriptionServiceException( "The transcription provider returned an empty response.", null);
            }

            // Some provider responses may omit usage, so safely fall back to zero. condition ? valueIfTrue : valueIfFalse 
            long inputTokens = response.usage() == null ? 0 : response.usage().inputTokens();
            long outputTokens = response.usage() == null ? 0 : response.usage().outputTokens();
            return new TranscriptionResult(response.text(), inputTokens, outputTokens);
        } catch (RestClientResponseException exception) {
            // This catch handles an HTTP error response, such as 400 or 500.
            // The server log receives the technical cause. browser receives a safe message.
            throw new TranscriptionServiceException(
                    "The transcription provider rejected the request.", exception);
        } catch (ResourceAccessException exception) {
            // This catch handles CONNECTION PROBLEMS, TIMEOUTS, or an UNREACHABLE PROVIDER.
            throw new TranscriptionServiceException(
                    "The transcription provider could not be reached.", exception);
        }
    }

    /** Gives the multipart audio part a filename, which the OpenAI API requires. */
    // static means it does not need an OpenAiTranscriptionService object. final prevents further subclassing.
    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String fileName;

        private NamedByteArrayResource(byte[] bytes, String fileName) {
            // The parent ByteArrayResource stores the raw file bytes.
            super(bytes);
            this.fileName = fileName;
        }

        @Override
        public String getFilename() {
            // Overriding this method ensures the multipart file has its validated name
            return fileName;
        }
    }

    // Ignore extra provider fields so an additive API response change does not break parsing.
    @JsonIgnoreProperties(ignoreUnknown = true)
    // These private records model only the response fields this application needs
    private record OpenAiResponse(String text, OpenAiUsage usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiUsage(
            // @JsonProperty maps JSON snake_case names to Java lower-camel-case names.
            @JsonProperty("input_tokens") long inputTokens,
            @JsonProperty("output_tokens") long outputTokens) {
    }
}
