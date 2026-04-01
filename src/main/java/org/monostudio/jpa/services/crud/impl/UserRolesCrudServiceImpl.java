package org.monostudio.jpa.services.crud.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.UserRolePojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.UserRole;
import org.monostudio.jpa.repositories.UserRolesRepository;
import org.monostudio.jpa.services.conversion.UserRolesConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.UserRolesCrudService;
import org.monostudio.jpa.services.patch.UserRolesPatchService;

import java.util.Optional;

@Transactional
@Service
public class UserRolesCrudServiceImpl
    extends CrudGenericService<UserRolePojo, UserRole>
    implements UserRolesCrudService {
    private final UserRolesRepository rolesRepository;

    @Autowired
    public UserRolesCrudServiceImpl(
        UserRolesRepository rolesRepository,
        UserRolesConverterService rolesConverterService,
        UserRolesPatchService rolesPatchService
    ) {
        super(rolesRepository, rolesConverterService, rolesPatchService);
        this.rolesRepository = rolesRepository;
    }

    @Override
    public Optional<UserRole> getExisting(UserRolePojo input) throws BadInputException {
        String name = input.getName();
        if (StringUtils.isBlank(name)) {
            throw new BadInputException("Invalid user role name");
        } else {
            return rolesRepository.findByName(name);
        }
    }
}
