package cloud.concurrent.assignment1.admin;

import cloud.concurrent.assignment1.web.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/** Refuses new work while Spring is draining requests that were already in flight. */
// @Component registers the filter with Spring automatically.
@Component

// A low order number runs early HIGHEST_PRECEDENCE + 1 leaves room for Spring's
// own highest-priority filter while still checking shutdown before normal work
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
// extends = this class inherits OncePerRequestFilter's behaviour. 
// Spring then calls doFilterInternal exactly once for each HTTP request.
public class ShutdownDrainFilter extends OncePerRequestFilter {

    // static final creates one class-wide constant whose value cannot be reassigned
    private static final String SHUTDOWN_PATH = "/api/v1/admin/shutdown";

    private final ShutdownCoordinator shutdownCoordinator;
    // ObjectMapper converts Java objects, ex. ErrorResponse, into JSON.
    private final ObjectMapper objectMapper;

    // Spring constructor injection supplies both required collaborators
    public ShutdownDrainFilter(
            ShutdownCoordinator shutdownCoordinator,
            ObjectMapper objectMapper) {
        this.shutdownCoordinator = shutdownCoordinator;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Filters run before controllers. Existing work can finish, but once shutdown begins, new requests are stopped here before they start additional work
        
        // continue when shutdown has not started, or when this request is the shutdown request itself. 
        // String.equals compares the path's contents.
        if (!shutdownCoordinator.isShutdownRequested() || SHUTDOWN_PATH.equals(request.getRequestURI())) {
            // Calling the next filter continues the normal request-processing chain.
            filterChain.doFilter(request, response);
            return;
        }

        // Write the required JSON error directly bc request never reaches a controller.
        // HTTP 422 tells caller this normally valid request cannot be processed now.
        HttpStatus status = HttpStatus.valueOf(422);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // The ObjectMapper serialises ErrorResponse into the HTTP response output stream.
        objectMapper.writeValue(
                response.getOutputStream(),
                new ErrorResponse(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        "The server is shutting down and cannot accept new work.",
                        request.getRequestURI()));
    }
}
