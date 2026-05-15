package org.monostudio.api.services.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.monostudio.api.models.CompanyDetailsPojo;
import org.monostudio.api.models.SystemSettingsPojo;
import org.monostudio.api.services.CompanyService;
import org.monostudio.api.services.SystemSettingsService;
import org.monostudio.jpa.entities.Param;
import org.monostudio.jpa.repositories.ParamsRepository;

import java.util.Collections;
import java.util.List;

@Service
public class CompanyServiceImpl
    implements CompanyService {
    private final ParamsRepository paramsRepository;
    private final SystemSettingsService systemSettingsService;
    private final ObjectMapper objectMapper;

    @Autowired
    public CompanyServiceImpl(
        ParamsRepository paramsRepository,
        SystemSettingsService systemSettingsService,
        ObjectMapper objectMapper
    ) {
        this.paramsRepository = paramsRepository;
        this.systemSettingsService = systemSettingsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompanyDetailsPojo readDetails() {
        Iterable<Param> it = paramsRepository.findParamsByCategory("company");
        CompanyDetailsPojo target = new CompanyDetailsPojo();
        for (Param p : it) {
            String v = p.getValue();
            switch (p.getName()) {
                case "name":
                    target.setName(v);
                    break;
                case "description":
                    target.setDescription(v);
                    break;
                case "bannerImageURL":
                    target.setBannerImageURL(v);
                    break;
                case "bannerImageURLs":
                    target.setBannerImageURLs(parseBannerImageUrls(v));
                    break;
                case "logoImageURL":
                    target.setLogoImageURL(v);
                    break;
                default:
                    break;
            }
        }
        SystemSettingsPojo systemSettings = systemSettingsService.readSettings();
        target.setPhone(systemSettings.getSupportPhone());
        target.setEmail(systemSettings.getSupportEmail());
        target.setMaintenanceMode(systemSettings.isMaintenanceMode());
        return target;
    }

    private List<String> parseBannerImageUrls(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(rawValue, new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}
