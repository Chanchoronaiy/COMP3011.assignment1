package cloud.concurrent.assignment1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/** Configures the reusable HTTP client used only by the Java backend. */
// @Configuration tells Spring that this class contains methods that create
// application objects. The browser never receives this class or the API key.
@Configuration
public class OpenAiConfiguration {

    /**
     * Creates one reusable client rather than constructing a new client for every request.
     * Reuse allows the underlying HTTP connection pool to reduce network overhead.
     */
    // @Bean tells Spring to call this method and manage the returned RestClient.
    @Bean
    // There is no public/private keyword, so this method has package-private access.
    // Spring supplies both method parameters through dependency injection.
    RestClient openAiRestClient(RestClient.Builder builder, OpenAiProperties properties) {
        // Spring passes the builder and properties into this method. This is dependency
        // injection: this class uses objects supplied by Spring instead of constructing them.
        // The builder pattern collects configuration step by step before build() creates
        // the finished, reusable RestClient object.
        RestClient.Builder configuredBuilder = builder.baseUrl(properties.baseUrl());

        // Do not create an Authorization header containing a blank key during local development.
        // The key is added only to requests sent by the backend; it is never sent to the browser.
        if (properties.hasApiKey()) {
            configuredBuilder.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    // + joins the authentication scheme and secret key into one String.
                    "Bearer " + properties.apiKey());
        }

        // @Bean stores the returned client in Spring's application context. Other classes can
        // then request the same configured RestClient in their constructors.
        return configuredBuilder.build();
    }
}
