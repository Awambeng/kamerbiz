package cm.kamerbiz.catalog;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class ProductImageValidatorTest {
  private static final byte[] PNG =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/pWQAAAAASUVORK5CYII=");

  private final ProductImageValidator validator = new ProductImageValidator();

  @Test
  void acceptsACompleteDecodablePng() {
    assertThatCode(() -> validator.validate(PNG, "image/png")).doesNotThrowAnyException();
  }

  @Test
  void rejectsTruncatedImageWithValidPngSignature() {
    assertThatThrownBy(() ->
            validator.validate(new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0, 0, 0, 0}, "image/png"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsContentTypeThatDoesNotMatchDecodedFormat() {
    assertThatThrownBy(() -> validator.validate(PNG, "image/jpeg"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
