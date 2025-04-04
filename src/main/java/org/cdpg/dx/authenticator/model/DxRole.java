package org.cdpg.dx.authenticator.model;

import java.util.stream.Stream;

public enum DxRole {
  
  CONSUMER("consumer"),
  PROVIDER("provider"),
  DELEGATE("delegate"),
  ADMIN("admin");

  private final String role;

  DxRole(String role) {
    this.role = role;
  }

  public static DxRole fromString(String role) {
    return Stream.of(values())
            .filter(v -> v.role.equalsIgnoreCase(role))
            .findAny()
            .orElse(null);
  }

  public String getRole() {
    return this.role;
  }

  public static DxRole fromRole(final JwtData jwtData) {
    return Stream.of(values())
        .filter(v -> v.role.equalsIgnoreCase(jwtData.getRole()))
        .findAny()
        .orElse(null);
  }

  public static DxRole fromRole(final DxRole dxRole) {
    return Stream.of(values())
            .filter(v -> v.role.equalsIgnoreCase(dxRole.getRole()))
            .findAny()
            .orElse(null);
  }


}