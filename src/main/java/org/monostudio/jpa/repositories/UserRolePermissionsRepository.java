package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.UserRolePermission;

import java.util.List;

@org.springframework.stereotype.Repository
public interface UserRolePermissionsRepository
    extends Repository<UserRolePermission> {

    @Query("SELECT urp FROM UserRolePermission urp JOIN FETCH urp.permission WHERE urp.userRole.id = :userRoleId")
    List<UserRolePermission> deepFindPermissionsByUserRoleId(@Param("userRoleId") Long userRoleId);

    @Query("SELECT urp FROM UserRolePermission urp JOIN FETCH urp.permission JOIN FETCH urp.userRole")
    List<UserRolePermission> findAllWithRoleAndPermission();
}
