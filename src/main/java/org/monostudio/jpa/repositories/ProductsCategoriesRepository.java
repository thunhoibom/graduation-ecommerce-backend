package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.ProductCategory;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ProductsCategoriesRepository
    extends Repository<ProductCategory> {

    Optional<ProductCategory> findByCode(String code);

    List<ProductCategory> findByName(String code);

    List<ProductCategory> findByParent(ProductCategory parent);

    @Query("SELECT r.id FROM ProductCategory r WHERE r.parent.id = :parentId")
    List<Long> findIdsByParentId(@Param("parentId") Long parentId);
}
