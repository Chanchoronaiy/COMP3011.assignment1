package cloud.concurrent.assignment1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Holds settings used to contact the OpenAI transcription API.
 *
 * API key comes from an environment variable through application.properties. Keeping it
 * outside the source code prevents it from being committed to Git or sent to the browser.
 */
// asks Spring to bind settings beginning with "openai" from application.properties or environment variables into this Java record.
@ConfigurationProperties(prefix = "openai")

// Java creates the constructor and accessor methods ex. apiKey(), model() and baseUrl() automatically
public record OpenAiProperties(
        String apiKey,
        // @DefaultValue is used when the matching configuration value is not supplied.
        @DefaultValue("gpt-4o-mini-transcribe") String model,
        @DefaultValue("https://api.openai.com") String baseUrl) {

    /** returns only when a usable server-side API key has been supplied. */
    public boolean hasApiKey() {
        // stops after the first false condition so isBlank() is never called on null (short-circuiting), also rejects empty key or whitespace.
        return apiKey != null && !apiKey.isBlank();
    }
}
