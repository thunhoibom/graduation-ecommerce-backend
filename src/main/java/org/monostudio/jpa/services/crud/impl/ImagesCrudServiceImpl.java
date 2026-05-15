package org.monostudio.jpa.services.crud.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ImagePojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Image;
import org.monostudio.jpa.repositories.ImagesRepository;
import org.monostudio.jpa.services.conversion.ImagesConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.ImagesCrudService;
import org.monostudio.jpa.services.patch.ImagesPatchService;

import java.util.Optional;

@Transactional
@Service
public class ImagesCrudServiceImpl
    extends CrudGenericService<ImagePojo, Image>
    implements ImagesCrudService {
    private final ImagesRepository imagesRepository;

    @Autowired
    public ImagesCrudServiceImpl(
        ImagesRepository imagesRepository,
        ImagesConverterService imagesConverterService,
        ImagesPatchService imagesPatchService
    ) {
        super(imagesRepository, imagesConverterService, imagesPatchService);
        this.imagesRepository = imagesRepository;
    }

    @Override
    public Optional<Image> getExisting(ImagePojo input) throws BadInputException {
        if (StringUtils.isNotBlank(input.getCode())) {
            return imagesRepository.findByCode(input.getCode());
        }
        if (StringUtils.isNotBlank(input.getFilename())) {
            return imagesRepository.findByFilename(input.getFilename());
        }
        throw new BadInputException("Invalid image metadata: both code and filename are missing");
    }

}
