package iudx.resource.server.apiserver.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.validation.types.PaginationOffsetTypeValidator;

@ExtendWith(VertxExtension.class)
public class PaginationOffsetTypeValidatorTest {

  private PaginationOffsetTypeValidator paginationOffsetTypeValidator;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    testContext.completeNow();
  }

  static Stream<Arguments> allowedValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of(null, false),
        Arguments.of("1000", false),
        Arguments.of("5000", false),
        Arguments.of("2500", false),
        Arguments.of("0", false));
  }

  @ParameterizedTest
  @MethodSource("allowedValues")
  @Description("pagination offset type parameter allowed values.")
  public void testValidOffsetTypeValue(
      String value, boolean required, Vertx vertx, VertxTestContext testContext) {
    paginationOffsetTypeValidator = new PaginationOffsetTypeValidator(value, required);
    assertTrue(paginationOffsetTypeValidator.isValid());
    testContext.completeNow();
  }

  static Stream<Arguments> invalidValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("-1", false),
        Arguments.of("50001", false),
        Arguments.of("   ", false),
        Arguments.of("7896541233568796313611634", false),
        Arguments.of("false", false),
        Arguments.of("kajlksdjloasknfdlkanslodnmalsdasd", false),
        Arguments.of(null, true),
        Arguments.of("", true));
  }

  @ParameterizedTest
  @MethodSource("invalidValues")
  @Description("pagination offset type parameter invalid values.")
  public void testInvalidOffsetTypeValue(
      String value, boolean required, Vertx vertx, VertxTestContext testContext) {
    paginationOffsetTypeValidator = new PaginationOffsetTypeValidator(value, required);
    assertThrows(DxRuntimeException.class, () -> paginationOffsetTypeValidator.isValid());
    testContext.completeNow();
  }
}
