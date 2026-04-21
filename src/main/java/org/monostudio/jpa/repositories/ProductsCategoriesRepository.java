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

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"image", "parent", "parent.image"})
    Optional<ProductCategory> findByCode(String code);

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"image", "parent", "parent.image"})
    java.util.List<ProductCategory> findAll();

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"image", "parent", "parent.image"})
    org.springframework.data.domain.Page<ProductCategory> findAll(org.springframework.data.domain.Pageable pageable);

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"image", "parent", "parent.image"})
    org.springframework.data.domain.Page<ProductCategory> findAll(com.querydsl.core.types.Predicate predicate, org.springframework.data.domain.Pageable pageable);

    List<ProductCategory> findByName(String code);

    List<ProductCategory> findByParent(ProductCategory parent);

    @Query("SELECT r.id FROM ProductCategory r WHERE r.parent.id = :parentId")
    List<Long> findIdsByParentId(@Param("parentId") Long parentId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.productCategory.id = :categoryId")
    long countProductsByCategoryId(@Param("categoryId") Long categoryId);
}
