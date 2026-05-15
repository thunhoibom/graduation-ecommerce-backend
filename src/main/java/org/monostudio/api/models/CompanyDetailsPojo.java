package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;

@Data
@JsonInclude(ALWAYS)
public class CompanyDetailsPojo {
    private String name;
    private String description;
    private String bannerImageURL;
    private List<String> bannerImageURLs;
    private String logoImageURL;
    private String phone;
    private String email;
    private boolean maintenanceMode;
}
