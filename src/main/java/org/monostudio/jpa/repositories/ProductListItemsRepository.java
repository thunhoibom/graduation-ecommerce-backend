package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.ProductListItem;

@org.springframework.stereotype.Repository
public interface ProductListItemsRepository
    extends Repository<ProductListItem> {

    @Modifying
    @Transactional
    @Query("DELETE FROM ProductListItem pi WHERE pi.list.id = :id")
    void deleteByListId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("DELETE FROM ProductListItem pi WHERE pi.product.id = :id")
    void deleteByProductId(@Param("id") Long id);
}
