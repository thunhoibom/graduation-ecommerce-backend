package org.monostudio.jpa.services.conversion.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ParamPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Param;
import org.monostudio.jpa.services.conversion.ParamsConverterService;

@Transactional
@Service
public class ParamsConverterServiceImpl
    implements ParamsConverterService {

    @Override
    public ParamPojo convertToPojo(Param source) {
        return ParamPojo.builder()
            .id(source.getId())
            .category(source.getCategory())
            .name(source.getName())
            .value(source.getValue())
            .build();
    }

    @Override
    public Param convertToNewEntity(ParamPojo source) throws BadInputException {
        return Param.builder()
            .category(source.getCategory())
            .name(source.getName())
            .value(source.getValue())
            .build();
    }

    @Override
    public Param applyChangesToExistingEntity(ParamPojo source, Param target) {
        // Only update value — category and name are the identifying key
        if (source.getValue() != null) {
            target.setValue(source.getValue());
        }
        if (source.getCategory() != null) {
            target.setCategory(source.getCategory());
        }
        if (source.getName() != null) {
            target.setName(source.getName());
        }
        return target;
    }
}