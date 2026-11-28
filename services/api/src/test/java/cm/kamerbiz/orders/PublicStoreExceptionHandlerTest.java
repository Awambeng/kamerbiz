package cm.kamerbiz.orders;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PublicStoreExceptionHandlerTest {
  private final PublicStoreExceptionHandler handler = new PublicStoreExceptionHandler();

  @Test
  void returnsJsonNotFoundWithoutAnOauthChallenge() {
    var response = handler.handleStatus(new ResponseStatusException(HttpStatus.NOT_FOUND));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getHeaders()).doesNotContainKey(HttpHeaders.WWW_AUTHENTICATE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(404);
    assertThat(response.getBody().message()).isEqualTo("Not Found");
  }

  @Test
  void preservesPublicValidationAndStockConflictStatuses() {
    var invalid =
        handler.handleStatus(
            new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery address is required."));
    var conflict =
        handler.handleStatus(
            new ResponseStatusException(
                HttpStatus.CONFLICT, "A product no longer has enough stock."));

    assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(invalid.getBody().message()).isEqualTo("Delivery address is required.");
    assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(conflict.getBody().message()).isEqualTo("A product no longer has enough stock.");
  }
}
