package cm.kamerbiz.catalog;

import java.util.ArrayList;
import java.util.List;

final class ProductCsvParser {
  private ProductCsvParser() {}

  static List<List<String>> parse(String content) {
    List<List<String>> rows = new ArrayList<>();
    List<String> row = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < content.length(); i++) {
      char current = content.charAt(i);
      if (quoted) {
        if (current == '"' && i + 1 < content.length() && content.charAt(i + 1) == '"') {
          field.append('"');
          i++;
        } else if (current == '"') {
          quoted = false;
        } else {
          field.append(current);
        }
      } else if (current == '"' && field.isEmpty()) {
        quoted = true;
      } else if (current == ',') {
        row.add(field.toString().trim());
        field.setLength(0);
      } else if (current == '\n' || current == '\r') {
        if (current == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') i++;
        row.add(field.toString().trim());
        field.setLength(0);
        if (!(row.size() == 1 && row.get(0).isBlank())) rows.add(row);
        row = new ArrayList<>();
      } else {
        field.append(current);
      }
    }
    if (quoted) throw new IllegalArgumentException("CSV contains an unclosed quoted field.");
    if (!field.isEmpty() || !row.isEmpty()) {
      row.add(field.toString().trim());
      rows.add(row);
    }
    return rows;
  }
}
