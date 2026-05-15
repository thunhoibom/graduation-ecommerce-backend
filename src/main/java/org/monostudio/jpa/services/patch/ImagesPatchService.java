package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.ImagePojo;
import org.monostudio.jpa.entities.Image;
import org.monostudio.jpa.services.PatchService;

public interface ImagesPatchService
    extends PatchService<ImagePojo, Image> {
}
