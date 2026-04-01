package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.UserPojo;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.services.ConverterService;

public interface UsersConverterService
    extends ConverterService<UserPojo, User> {
}
