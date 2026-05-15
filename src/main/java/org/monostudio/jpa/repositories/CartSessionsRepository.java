package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.CartSession;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface CartSessionsRepository
    extends Repository<CartSession> {

    Optional<CartSession> findByToken(String token);

    @Query("SELECT c FROM CartSession c LEFT JOIN FETCH c.items i "
        + "LEFT JOIN FETCH i.variant v LEFT JOIN FETCH v.product "
        + "WHERE c.token = :token")
    Optional<CartSession> findByTokenDeep(@Param("token") String token);

    boolean existsByToken(String token);
}
