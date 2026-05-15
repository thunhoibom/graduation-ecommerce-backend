package org.monostudio.jpa.services.conversion.impl;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.UserRolePojo;
import org.monostudio.jpa.entities.UserRole;
import org.monostudio.jpa.services.conversion.UserRolesConverterService;

@Service
@NoArgsConstructor
public class UserRolesConverterServiceImpl
    implements UserRolesConverterService {

    @Override
    public UserRolePojo convertToPojo(UserRole source) {
        return UserRolePojo.builder()
            .id(source.getId())
            .name(source.getName())
            .build();
    }

    @Override
    public UserRole convertToNewEntity(UserRolePojo source) {
        return UserRole.builder()
            .name(source.getName())
            .build();
    }

    @Override
    public UserRole applyChangesToExistingEntity(UserRolePojo source, UserRole target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
