package org.monostudio.jpa.services.crud;

import org.monostudio.api.models.ImagePojo;
import org.monostudio.jpa.entities.Image;
import org.monostudio.jpa.services.CrudService;

public interface ImagesCrudService
    extends CrudService<ImagePojo, Image> {
}
