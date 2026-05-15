package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemSettingsPojo {
    private String supportEmail;
    private String supportPhone;
    private boolean maintenanceMode;
}
