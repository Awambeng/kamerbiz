package cm.kamerbiz.catalog;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

final class ProductImageValidator {
  // Bound decoded image memory before allocating the full raster.
  private static final long MAX_PIXELS = 16_000_000;

  void validate(byte[] bytes, String contentType) {
    try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      if (input == null) throw new IllegalArgumentException("Image data is unreadable.");
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) throw new IllegalArgumentException("Unsupported image data.");

      ImageReader reader = readers.next();
      try {
        String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
        if (!matchesContentType(format, contentType)) {
          throw new IllegalArgumentException("Image content does not match its file type.");
        }
        reader.setInput(input, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width < 1 || height < 1 || (long) width * height > MAX_PIXELS) {
          throw new IllegalArgumentException("Image dimensions are not supported.");
        }
        BufferedImage decoded = reader.read(0);
        if (decoded == null) throw new IllegalArgumentException("Image could not be decoded.");
      } finally {
        reader.dispose();
      }
    } catch (IOException | RuntimeException exception) {
      throw new IllegalArgumentException("Only valid JPEG, PNG and WebP images are supported.");
    }
  }

  private boolean matchesContentType(String format, String contentType) {
    return ("image/jpeg".equalsIgnoreCase(contentType) && format.equals("jpeg"))
        || ("image/png".equalsIgnoreCase(contentType) && format.equals("png"))
        || ("image/webp".equalsIgnoreCase(contentType) && format.equals("webp"));
  }
}
