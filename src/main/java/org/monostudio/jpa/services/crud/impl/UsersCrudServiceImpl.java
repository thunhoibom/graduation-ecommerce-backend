package org.monostudio.jpa.services.crud.impl;

import com.querydsl.core.types.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.UserPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.config.SecurityProperties;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.jpa.services.conversion.UsersConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.UsersCrudService;
import org.monostudio.jpa.services.patch.UsersPatchService;
import org.monostudio.security.exceptions.AccountProtectionViolationException;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;

@Transactional
@Service
public class UsersCrudServiceImpl
    extends CrudGenericService<UserPojo, User>
    implements UsersCrudService {
    private final UsersRepository usersRepository;
    private final SecurityProperties securityProperties;

    @Autowired
    public UsersCrudServiceImpl(
        UsersRepository usersRepository,
        UsersConverterService usersConverterService,
        UsersPatchService usersPatchService,
        SecurityProperties securityProperties
    ) {
        super(usersRepository, usersConverterService, usersPatchService);
        this.usersRepository = usersRepository;
        this.securityProperties = securityProperties;
    }

    @Override
    public Optional<User> getExisting(UserPojo input) throws BadInputException {
        String name = input.getName();
        if (StringUtils.isBlank(name)) {
            throw new BadInputException("Invalid user name");
        } else {
            return usersRepository.findByName(name);
        }
    }

    @Override
    public void delete(Predicate filters) throws EntityNotFoundException {
        if (securityProperties.isAccountProtectionEnabled()) {
            Optional<User> optionalUser = usersRepository.findOne(filters);
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                if (user.getId()==securityProperties.getProtectedAccountId()) {
                    throw new AccountProtectionViolationException("Protected account cannot be deleted");
                }
            }
        }
        super.delete(filters);
    }
}
