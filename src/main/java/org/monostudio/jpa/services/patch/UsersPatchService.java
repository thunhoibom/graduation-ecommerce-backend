package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.UserPojo;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.services.PatchService;

public interface UsersPatchService
    extends PatchService<UserPojo, User> {
}
