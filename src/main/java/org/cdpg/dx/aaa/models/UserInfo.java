package org.cdpg.dx.aaa.models;

import java.util.UUID;
import org.cdpg.dx.common.models.DxRole;

/**
 * Immutable UserInfo model.
 */
public record UserInfo(UUID userId, boolean isDelegate, DxRole role, String audience) {
}
