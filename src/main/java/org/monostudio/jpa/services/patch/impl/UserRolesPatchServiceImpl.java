package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.UserRolePojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.UserRole;
import org.monostudio.jpa.services.patch.UserRolesPatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class UserRolesPatchServiceImpl
    implements UserRolesPatchService {

    @Override
    public UserRole patchExistingEntity(Map<String, Object> changes, UserRole existing) throws BadInputException {
        UserRole target = new UserRole(existing);

        if (changes.containsKey("name")) {
            String name = (String) changes.get("name");
            if (!StringUtils.isBlank(name)) {
                target.setName(name);
            }
        }

        return target;
    }

    @Override
    public UserRole patchExistingEntity(UserRolePojo changes, UserRole existing) throws BadInputException {
        throw new UnsupportedOperationException("This method has been deprecated");
    }
}
