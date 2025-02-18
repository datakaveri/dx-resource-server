package iudx.resource.server.common.validation.types;

public interface Validator {

  boolean isValid();

  int failureCode();

  String failureMessage();

  default String failureMessage(final String value) {
    return failureMessage() + " [ " + value + " ] ";
  }
}
