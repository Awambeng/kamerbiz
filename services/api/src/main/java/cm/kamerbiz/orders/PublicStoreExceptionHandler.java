package cm.kamerbiz.orders;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = PublicStoreController.class)
class PublicStoreExceptionHandler {
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<PublicApiError> handleStatus(ResponseStatusException exception) {
    HttpStatusCode status = exception.getStatusCode();
    HttpStatus knownStatus = HttpStatus.resolve(status.value());
    String message = exception.getReason();
    if (message == null || message.isBlank()) {
      message = knownStatus == null ? "Request failed." : knownStatus.getReasonPhrase();
    }
    String error = knownStatus == null ? "Request failed" : knownStatus.getReasonPhrase();
    return ResponseEntity.status(status).body(new PublicApiError(status.value(), error, message));
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  ResponseEntity<PublicApiError> handleInvalidRequest(Exception exception) {
    return ResponseEntity.badRequest()
        .body(new PublicApiError(400, "Bad Request", "The request is invalid."));
  }

  record PublicApiError(int status, String error, String message) {}
}
