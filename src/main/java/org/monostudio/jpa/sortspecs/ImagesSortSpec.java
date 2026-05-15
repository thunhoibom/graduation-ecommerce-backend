package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QImage;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ImagesSortSpec {
    private static final QImage BASE_PATH = QImage.image;
    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "code", BASE_PATH.code.asc(),
        "filename", BASE_PATH.filename.asc(),
        "url", BASE_PATH.url.asc()
    );
}
