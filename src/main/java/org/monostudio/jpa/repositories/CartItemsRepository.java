package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.entities.ProductVariant;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface CartItemsRepository
    extends Repository<CartItem> {

    List<CartItem> findByCartSessionToken(String sessionToken);

    @Query("SELECT i FROM CartItem i "
        + "JOIN FETCH i.variant v "
        + "JOIN FETCH v.product p "
        + "WHERE i.cartSession.token = :sessionToken")
    List<CartItem> findByCartSessionTokenDeep(@Param("sessionToken") String sessionToken);

    Optional<CartItem> findByCartSessionTokenAndVariantSku(String sessionToken, String variantSku);

    Optional<CartItem> findByCartSessionIdAndVariantId(Long cartSessionId, Long variantId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem i WHERE i.cartSession.token = :sessionToken")
    int deleteAllBySessionToken(@Param("sessionToken") String sessionToken);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem i WHERE i.cartSession.id = :sessionId")
    int deleteAllBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT SUM(i.quantity) FROM CartItem i WHERE i.cartSession.token = :sessionToken")
    Integer sumQuantityBySessionToken(@Param("sessionToken") String sessionToken);
}
