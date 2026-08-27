package cloud.concurrent.assignment1.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Accepts the graceful-shutdown operation defined by assignment1api.yaml. */
// @RestController tells Spring that this class receives HTTP requests and that
// its return values should be written directly into the HTTP response body.
@RestController
// Every endpoint in this class starts with /api/v1/admin.
@RequestMapping("/api/v1/admin")
public class ShutdownController {

    // final means the field must be assigned once and cannot later point at a
    // different coordinator. This makes the controller's dependency stable.
    private final ShutdownCoordinator shutdownCoordinator;

    // Spring automatically supplies an object that implements ShutdownCoordinator.
    // This is called constructor injection; no "new" is needed in this class.
    public ShutdownController(ShutdownCoordinator shutdownCoordinator) {
        // The controller depends on the interface, not the concrete Spring implementation.
        // This is Spring constructor injection and keeps the class straightforward to test.
        this.shutdownCoordinator = shutdownCoordinator;
    }

    // @PostMapping connects HTTP POST /api/v1/admin/shutdown to this method.
    @PostMapping("/shutdown")
    // ResponseEntity lets the method choose both the HTTP status and response body.
    public ResponseEntity<ShutdownResponse> shutDown() {
        // Only the first caller changes the shared shutdown state. Later callers receive 409.
        // The ! operator reverses a boolean: false becomes true and enters this block.
        if (!shutdownCoordinator.requestShutdown()) {
            // throw stops normal execution and passes this problem to the API error handler.
            throw new ShutdownInProgressException();
        }

        // HTTP 202 means the request was accepted and shutdown will complete asynchronously.
        return ResponseEntity
                .accepted()
                .body(new ShutdownResponse("Graceful shutdown requested."));
    }

    /**
     * The response contains exactly the one property specified by the supplied API contract.
     * A record is a compact Java data carrier; Java creates its constructor and
     * {@code message()} accessor automatically.
     */
    public record ShutdownResponse(String message) {
    }
}
