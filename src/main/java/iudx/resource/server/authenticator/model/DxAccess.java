package iudx.resource.server.authenticator.model;

import java.util.stream.Stream;

public enum DxAccess {

  API("api"),
  SUBSCRIPTION("sub"),
  INGESTION("ingestion"),
  MANAGEMENT("management"),
  ASYNC("async");

  private final String access;

  DxAccess(String access) {
    this.access = access;
  }

  public String getAccess() {
    return this.access;
  }

  public static DxAccess fromAccess(final String constraint) {
    return Stream.of(values())
            .filter(v -> v.access.equalsIgnoreCase(constraint))
            .findAny()
            .orElse(null);
  }

}
