package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.CompanyDetailsPojo;
import org.monostudio.api.services.CompanyService;
import org.monostudio.jpa.entities.Param;
import org.monostudio.jpa.repositories.ParamsRepository;

@Service
public class CompanyServiceImpl
    implements CompanyService {
    private final ParamsRepository paramsRepository;

    @Autowired
    public CompanyServiceImpl(
        ParamsRepository paramsRepository
    ) {
        this.paramsRepository = paramsRepository;
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
                case "logoImageURL":
                    target.setLogoImageURL(v);
                    break;
                default:
                    break;
            }
        }
        return target;
    }
}
