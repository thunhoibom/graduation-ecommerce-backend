package org.monostudio.jpa.services.patch.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.UserPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.repositories.UserRolesRepository;
import org.monostudio.jpa.services.patch.UsersPatchService;

import java.util.Map;

@Transactional
@Service
public class UsersPatchServiceImpl
    implements UsersPatchService {
    private final UserRolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;

    public UsersPatchServiceImpl(
        UserRolesRepository rolesRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.rolesRepository = rolesRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User patchExistingEntity(Map<String, Object> changes, User existing) throws BadInputException {
        User target = new User(existing);

        if (changes.containsKey("name")) {
            String name = (String) changes.get("name");
            if (!StringUtils.isBlank(name)) {
                target.setName(name);
            }
        }

        if (changes.containsKey("role")) {
            String roleName = (String) changes.get("role");
            if (!StringUtils.isBlank(roleName)) {
                rolesRepository.findByName(roleName).ifPresent(target::setUserRole);
            }
        }

        if (changes.containsKey("password")) {
            String password = (String) changes.get("password");
            if (!StringUtils.isBlank(password)) {
                target.setPassword(passwordEncoder.encode(password));
            }
        }

        return target;
    }

    @Override
    public User patchExistingEntity(UserPojo changes, User existing) throws BadInputException {
        throw new UnsupportedOperationException("This method signature has been deprecated");
    }
}
