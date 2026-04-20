package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.VariantImage;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface VariantImagesRepository
    extends Repository<VariantImage> {

    List<VariantImage> findByVariantId(Long variantId);

    @Query("SELECT vi FROM VariantImage vi JOIN FETCH vi.image WHERE vi.variant.id = :variantId ORDER BY vi.sortOrder ASC")
    List<VariantImage> deepFindByVariantId(@Param("variantId") Long variantId);

    @Query("SELECT vi FROM VariantImage vi JOIN FETCH vi.image WHERE vi.variant.id = :variantId AND vi.isPrimary = true")
    Optional<VariantImage> findPrimaryByVariantId(@Param("variantId") Long variantId);

    @Modifying
    @Query("DELETE FROM VariantImage vi WHERE vi.variant.id = :variantId")
    int deleteByVariantId(@Param("variantId") Long variantId);

    @Query("SELECT vi FROM VariantImage vi JOIN FETCH vi.image WHERE vi.variant.id IN :variantIds AND vi.isPrimary = true")
    List<VariantImage> findPrimaryByVariantIds(@Param("variantIds") java.util.Collection<Long> variantIds);

    @Query("SELECT vi FROM VariantImage vi JOIN FETCH vi.image WHERE vi.variant.id IN :variantIds ORDER BY vi.sortOrder ASC")
    List<VariantImage> findByVariantIdIn(@Param("variantIds") java.util.Collection<Long> variantIds);
}
