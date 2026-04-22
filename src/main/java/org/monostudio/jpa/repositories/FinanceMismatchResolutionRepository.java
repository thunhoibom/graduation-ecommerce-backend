package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.FinanceMismatchResolution;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface FinanceMismatchResolutionRepository
    extends Repository<FinanceMismatchResolution> {
    Optional<FinanceMismatchResolution> findByMismatchKey(String mismatchKey);

    List<FinanceMismatchResolution> findByMismatchKeyIn(Collection<String> mismatchKeys);
}
