package org.monostudio.jpa.services;

import com.querydsl.core.types.Predicate;

import java.util.Map;

/**
 * An interface for parsing Predicates for use in JPA queries
 */
public interface PredicateService {

    /**
     * Reads Map and creates filtering conditions based on its data
     *
     * @param queryParamsMap A map of keys and values
     * @return A Predicate with filtering conditions as parsed from the input map
     */
    Predicate parseMap(Map<String, String> queryParamsMap);
}
