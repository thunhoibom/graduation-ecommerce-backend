package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.Image;
import org.monostudio.jpa.entities.QImage;
import org.monostudio.jpa.services.PredicateService;

public interface ImagesPredicateService
    extends PredicateService<Image> {
    QImage basePath = QImage.image;
}
