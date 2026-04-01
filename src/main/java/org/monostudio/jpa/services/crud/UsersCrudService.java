package org.monostudio.jpa.services.crud;

import org.monostudio.api.models.UserPojo;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.services.CrudService;

public interface UsersCrudService
    extends CrudService<UserPojo, User> {
}
