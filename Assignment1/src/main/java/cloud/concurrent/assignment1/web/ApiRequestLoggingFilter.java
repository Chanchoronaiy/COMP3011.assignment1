package cloud.concurrent.assignment1.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Adds a correlation identifier and one privacy-safe completion log to every API request. */
// @Component makes Spring discover and register this filter automatically.
@Component
// HIGHEST_PRECEDENCE runs this filter before other application filters. That means
// even a request rejected during shutdown still receives an ID and completion log.
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    // public allows tests and other classes to reuse the header name. static final
    // makes it one unchangeable class-level constant.
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Static files do not need API audit entries, so only /api/... requests are filtered.
        // startsWith checks the path prefix; ! reverses the result because the inherited
        // method asks when the filter should NOT run.
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // A unique ID connects logs from the same request without logging private request data.
        // UUID.randomUUID() makes a very unlikely-to-repeat identifier for each request.
        String requestId = UUID.randomUUID().toString();
        // nanoTime is designed for measuring elapsed time rather than displaying a date.
        long startNanoseconds = System.nanoTime();
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // MDC attaches the same identifier to any log produced while this request is handled.
        // try-with-resources automatically closes MDCCloseable and removes the temporary
        // value afterward, preventing one concurrent request's ID leaking into another.
        try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
            // Continue through the remaining filters and eventually the selected controller.
            filterChain.doFilter(request, response);
        } finally {
            // finally runs for both successful and failed requests, so timing is always logged.
            long durationMilliseconds =
                    (System.nanoTime() - startNanoseconds) / 1_000_000;
            // Deliberately exclude headers, bodies, audio, transcribed text, and credentials.
            // SLF4J replaces each {} placeholder with the corresponding value below.
            LOGGER.info(
                    "API request completed: requestId={} method={} path={} status={} durationMs={}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMilliseconds);
        }
    }
}
