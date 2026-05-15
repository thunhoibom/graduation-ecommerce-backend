package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.Shipper;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ShippersRepository
    extends Repository<Shipper> {

    Optional<Shipper> findByName(String name);
}
