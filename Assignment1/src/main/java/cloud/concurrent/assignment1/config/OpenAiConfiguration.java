package cloud.concurrent.assignment1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * Creates one reusable client rather than constructing a new client for every request
 * allowing underlying HTTP connection pool to reduce network overhead
 */

// @Configuration tells Spring that class contains methods that create application objects
@Configuration
public class OpenAiConfiguration {

    // @Bean stores the returned client in Spring's application context.
    @Bean

    //dependency injection: class uses objects supplied by Spring instead of constructing them
    RestClient openAiRestClient(RestClient.Builder builder, OpenAiProperties properties) {
        // builder collects configuration before build() creates the finished, reusable RestClient object
        RestClient.Builder configuredBuilder = builder.baseUrl(properties.baseUrl());

        // !!! Do not create an Authorization header containing a blank key during local development
        // The key is added only to requests sent by the backend. never sent to the browser
        if (properties.hasApiKey()) {
            configuredBuilder.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    //  authentication scheme and secret key into one string
                    "Bearer " + properties.apiKey());
        }

        // Spring injects this configured RestClient into any class that declares it in a constructor
        // then request the same configured RestClient in their constructors
        return configuredBuilder.build();
    }
}
