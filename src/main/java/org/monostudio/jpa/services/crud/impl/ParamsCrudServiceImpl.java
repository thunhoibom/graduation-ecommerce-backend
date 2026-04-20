package org.monostudio.jpa.services.crud.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ParamPojo;
import org.monostudio.jpa.entities.Param;
import org.monostudio.jpa.repositories.ParamsRepository;
import org.monostudio.jpa.services.conversion.ParamsConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.ParamsCrudService;
import org.monostudio.jpa.services.patch.ParamsPatchService;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;

@Transactional
@Service
public class ParamsCrudServiceImpl
    extends CrudGenericService<ParamPojo, Param>
    implements ParamsCrudService {

    private final ParamsRepository paramsRepository;

    @Autowired
    public ParamsCrudServiceImpl(
        ParamsRepository paramsRepository,
        ParamsConverterService paramsConverterService,
        ParamsPatchService paramsPatchService
    ) {
        super(paramsRepository, paramsConverterService, paramsPatchService);
        this.paramsRepository = paramsRepository;
    }

    @Override
    public Optional<Param> getExisting(ParamPojo input) throws org.monostudio.common.exceptions.BadInputException {
        if (input.getCategory() == null || input.getName() == null) {
            return Optional.empty();
        }
        return paramsRepository.findByCategoryAndName(input.getCategory(), input.getName());
    }

    @Override
    public ParamPojo findById(Long id) throws EntityNotFoundException {
        return paramsRepository.findById(id
        ).map(converter::convertToPojo
        ).orElseThrow(() -> new EntityNotFoundException(ITEM_NOT_FOUND));
    }
}