package cm.kamerbiz.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductCsvParserTest {
  @Test
  void parsesQuotedCommasAndEscapedQuotes() {
    var rows =
        ProductCsvParser.parse("name,description\nBlue Jeans,\"Classic, dark \"\"denim\"\"\"");
    assertThat(rows).hasSize(2);
    assertThat(rows.get(1)).containsExactly("Blue Jeans", "Classic, dark \"denim\"");
  }

  @Test
  void supportsWindowsLineEndings() {
    var rows = ProductCsvParser.parse("name,price\r\nSoap,1000\r\nOil,2500");
    assertThat(rows).hasSize(3);
  }
}
