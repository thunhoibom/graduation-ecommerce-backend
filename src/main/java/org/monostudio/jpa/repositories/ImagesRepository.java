package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.Image;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ImagesRepository
    extends Repository<Image> {

    Optional<Image> findByFilename(String filename);
    Optional<Image> findByCode(String code);
}

