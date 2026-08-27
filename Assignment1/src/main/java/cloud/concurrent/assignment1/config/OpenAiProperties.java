package cloud.concurrent.assignment1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Holds settings used to contact the OpenAI transcription API.
 *
 * <p>The API key comes from an environment variable through application.properties. Keeping it
 * outside the source code prevents it from being committed to Git or sent to the browser.</p>
 */
// @ConfigurationProperties asks Spring to bind settings beginning with "openai"
// from application.properties or environment variables into this Java record.
@ConfigurationProperties(prefix = "openai")
// A record is a compact immutable data carrier. Java creates the constructor and accessor
// methods such as apiKey(), model() and baseUrl() automatically.
public record OpenAiProperties(
        String apiKey,
        // @DefaultValue is used when the matching configuration value is not supplied.
        @DefaultValue("gpt-4o-mini-transcribe") String model,
        @DefaultValue("https://api.openai.com") String baseUrl) {

    /** Returns true only when a usable server-side API key has been supplied. */
    public boolean hasApiKey() {
        // null means no String object was supplied. The && operator means both
        // conditions must be true. Java stops after the first false condition,
        // so isBlank() is never called on null (this is called short-circuiting).
        // isBlank() also rejects an empty key or one containing only whitespace.
        return apiKey != null && !apiKey.isBlank();
    }
}
