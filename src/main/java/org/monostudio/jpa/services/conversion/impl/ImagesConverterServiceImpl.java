package org.monostudio.jpa.services.conversion.impl;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ImagePojo;
import org.monostudio.jpa.entities.Image;
import org.monostudio.jpa.services.conversion.ImagesConverterService;

@Service
@NoArgsConstructor
public class ImagesConverterServiceImpl
    implements ImagesConverterService {

    @Override
    public ImagePojo convertToPojo(Image source) {
        return ImagePojo.builder()
            .id(source.getId())
            .code(source.getCode())
            .filename(source.getFilename())
            .url(source.getUrl())
            .altText(source.getAltText())
            .mimeType(source.getMimeType())
            .width(source.getWidth())
            .height(source.getHeight())
            .fileSize(source.getFileSize())
            .build();
    }

    @Override
    public Image convertToNewEntity(ImagePojo source) {
        return Image.builder()
            .code(source.getCode())
            .filename(source.getFilename())
            .url(source.getUrl())
            .altText(source.getAltText())
            .mimeType(source.getMimeType())
            .width(source.getWidth())
            .height(source.getHeight())
            .fileSize(source.getFileSize())
            .build();
    }

    @Override
    public Image applyChangesToExistingEntity(ImagePojo source, Image target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
