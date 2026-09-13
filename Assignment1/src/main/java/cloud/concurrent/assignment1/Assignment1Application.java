package cloud.concurrent.assignment1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Starts the Spring Boot web application.
 *
 * The main method is deliberately small because Spring creates and connects the controllers,
 * services, and configuration objects elsewhere through dependency injection.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Assignment1Application {

    public static void main(String[] args) {
        // SpringApplication.run starts the web server and creates every @Component,
        // @Service, @Configuration and @RestController used by the application
        SpringApplication.run(Assignment1Application.class, args);
    }
}
