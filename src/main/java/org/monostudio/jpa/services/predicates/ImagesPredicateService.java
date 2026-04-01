package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QImage;
import org.monostudio.jpa.services.PredicateService;

public interface ImagesPredicateService
    extends PredicateService {
    QImage basePath = QImage.image;
}
