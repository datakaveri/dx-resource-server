package org.cdpg.dx.common.models;

import java.util.Arrays;
import java.util.Optional;

public enum DxRole {
  CONSUMER("consumer"),
  PROVIDER("provider"),
  DELEGATE("delegate");

  private final String role;

  DxRole(String role) {
    this.role = role;
  }

  public String getRole() {
    return role;
  }

  @Override
  public String toString() {
    return role;
  }

  /**
   * Returns an Optional of DxRole based on the given role string.
   *
   * @param role the role string
   * @return Optional containing the matching DxRole, or empty if not found
   */
  public static Optional<DxRole> fromString(String role) {
    if (role == null || role.isEmpty()) {
      return Optional.empty();
    }
    return Arrays.stream(DxRole.values())
            .filter(r -> r.role.equalsIgnoreCase(role))
            .findFirst();
  }
}
