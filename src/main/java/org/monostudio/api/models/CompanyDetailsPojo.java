package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;

@Data
@JsonInclude(ALWAYS)
public class CompanyDetailsPojo {
    private String name;
    private String description;
    private String bannerImageURL;
    private String logoImageURL;
}
