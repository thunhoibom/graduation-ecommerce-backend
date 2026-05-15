package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.UserRolePojo;
import org.monostudio.jpa.entities.UserRole;
import org.monostudio.jpa.services.PatchService;

public interface UserRolesPatchService
    extends PatchService<UserRolePojo, UserRole> {
}
