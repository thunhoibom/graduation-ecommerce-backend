package org.monostudio.security.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.entities.Permission;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.entities.UserRole;
import org.monostudio.jpa.entities.UserRolePermission;
import org.monostudio.jpa.repositories.UserRolePermissionsRepository;
import org.monostudio.security.services.UserPermissionsService;

import java.util.HashSet;
import java.util.Set;

/**
 * Service required by the DaoAuthenticationProvider bean.
 */
@Service
public class UserPermissionsServiceImpl
    implements UserPermissionsService {
    private final UserRolePermissionsRepository userRolePermissionsRepository;

    @Autowired
    public UserPermissionsServiceImpl(
        UserRolePermissionsRepository userRolePermissionsRepository
    ) {
        this.userRolePermissionsRepository = userRolePermissionsRepository;
    }

    @Override
    public Set<Permission> loadPermissionsForUser(User source) {
        UserRole sourceUserRole = source.getUserRole();
        Long userRoleId = sourceUserRole.getId();
        Iterable<UserRolePermission> userRolePermissions = userRolePermissionsRepository
            .deepFindPermissionsByUserRoleId(userRoleId);

        Set<Permission> targetList = new HashSet<>();
        for (UserRolePermission rolePermission : userRolePermissions) {
            Permission p = rolePermission.getPermission();
            targetList.add(p);
        }

        return targetList;
    }
}
