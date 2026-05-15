package org.monostudio.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

/**
 * Interface for JPA repositories with QueryDSL support
 *
 * @param <E> The entity class
 */
public interface Repository<E extends DBEntity>
    extends JpaRepository<E, Long>, QuerydslPredicateExecutor<E> {
}
