package org.monostudio.api.services;

import org.monostudio.api.models.UserRoleWithPermissionsPojo;

import java.util.List;

public interface UserRolePermissionsReadService {
    List<UserRoleWithPermissionsPojo> listRolesWithPermissions();
}
