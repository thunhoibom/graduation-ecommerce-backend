package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.ImagePojo;
import org.monostudio.jpa.entities.Image;
import org.monostudio.jpa.services.ConverterService;

public interface ImagesConverterService
    extends ConverterService<ImagePojo, Image> {
}
