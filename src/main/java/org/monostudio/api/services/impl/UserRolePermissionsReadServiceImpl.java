package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PermissionPojo;
import org.monostudio.api.models.UserRoleWithPermissionsPojo;
import org.monostudio.api.services.UserRolePermissionsReadService;
import org.monostudio.jpa.entities.Permission;
import org.monostudio.jpa.entities.UserRole;
import org.monostudio.jpa.entities.UserRolePermission;
import org.monostudio.jpa.repositories.UserRolePermissionsRepository;
import org.monostudio.jpa.repositories.UserRolesRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserRolePermissionsReadServiceImpl
    implements UserRolePermissionsReadService {
    private final UserRolePermissionsRepository userRolePermissionsRepository;
    private final UserRolesRepository userRolesRepository;

    @Autowired
    public UserRolePermissionsReadServiceImpl(
        UserRolePermissionsRepository userRolePermissionsRepository,
        UserRolesRepository userRolesRepository
    ) {
        this.userRolePermissionsRepository = userRolePermissionsRepository;
        this.userRolesRepository = userRolesRepository;
    }

    @Override
    public List<UserRoleWithPermissionsPojo> listRolesWithPermissions() {
        Map<Long, UserRoleWithPermissionsPojo> rolesById = new LinkedHashMap<>();

        for (UserRole userRole : userRolesRepository.findAll()) {
            rolesById.put(
                userRole.getId(),
                UserRoleWithPermissionsPojo.builder()
                    .id(userRole.getId())
                    .name(userRole.getName())
                    .permissions(new ArrayList<>())
                    .build()
            );
        }

        for (UserRolePermission rolePermission : userRolePermissionsRepository.findAllWithRoleAndPermission()) {
            UserRole userRole = rolePermission.getUserRole();
            Permission permission = rolePermission.getPermission();
            if (userRole == null || permission == null) {
                continue;
            }

            UserRoleWithPermissionsPojo role = rolesById.get(userRole.getId());
            if (role == null) {
                continue;
            }

            role.getPermissions().add(
                PermissionPojo.builder()
                    .code(permission.getCode())
                    .description(permission.getDescription())
                    .build()
            );
        }

        List<UserRoleWithPermissionsPojo> roles = new ArrayList<>(rolesById.values());
        roles.sort(Comparator.comparing(UserRoleWithPermissionsPojo::getName, String.CASE_INSENSITIVE_ORDER));
        for (UserRoleWithPermissionsPojo role : roles) {
            role.getPermissions().sort(Comparator.comparing(PermissionPojo::getCode, String.CASE_INSENSITIVE_ORDER));
        }
        return roles;
    }
}
