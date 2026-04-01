package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ImagePojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Image;
import org.monostudio.jpa.services.patch.ImagesPatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class ImagesPatchServiceImpl
    implements ImagesPatchService {

    @Override
    public Image patchExistingEntity(Map<String, Object> changes, Image existing) throws BadInputException {
        Image target = new Image(existing);

        if (changes.containsKey("code")) {
            String code = (String) changes.get("code");
            if (!StringUtils.isBlank(code)) {
                target.setCode(code);
            }
        }

        if (changes.containsKey("filename")) {
            String filename = (String) changes.get("filename");
            if (!StringUtils.isBlank(filename)) {
                target.setFilename(filename);
            }
        }

        if (changes.containsKey("url")) {
            String url = (String) changes.get("url");
            if (!StringUtils.isBlank(url)) {
                target.setUrl(url);
            }
        }

        return target;
    }

    @Override
    public Image patchExistingEntity(ImagePojo changes, Image target) throws BadInputException {
        throw new UnsupportedOperationException("This method signature has been deprecated");
    }
}
