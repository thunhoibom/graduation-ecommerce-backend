package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.ProductList;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ProductListsRepository
    extends Repository<ProductList> {

    Optional<ProductList> findByName(String name);
}
