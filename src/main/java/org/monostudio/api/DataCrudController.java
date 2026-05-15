package org.monostudio.api;

import org.monostudio.common.exceptions.BadInputException;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.Map;

public interface DataCrudController<M> {

    void create(@Valid M input) throws BadInputException, EntityExistsException;

    void update(@Valid M input, Map<String, String> requestParams) throws BadInputException, EntityNotFoundException;

    void partialUpdate(Map<String, Object> input, Map<String, String> requestParams) throws BadInputException, EntityNotFoundException;

    void delete(Map<String, String> requestParams) throws EntityNotFoundException;
}
