package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.GuestSession;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface GuestSessionsRepository
    extends Repository<GuestSession> {

    Optional<GuestSession> findBySessionUuid(String sessionUuid);

    Optional<GuestSession> findByCustomerId(Long customerId);

    boolean existsBySessionUuid(String sessionUuid);

    /**
     * Revoke a guest session (ban this specific session).
     * Does NOT affect other guest sessions.
     */
    @Modifying
    @Query("UPDATE GuestSession g SET g.revoked = :revoked WHERE g.sessionUuid = :sessionUuid")
    void updateRevokedBySessionUuid(@Param("sessionUuid") String sessionUuid, @Param("revoked") boolean revoked);
}