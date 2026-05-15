package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.SystemSettingsPojo;
import org.monostudio.api.services.SystemSettingsService;
import org.monostudio.jpa.entities.Param;
import org.monostudio.jpa.repositories.ParamsRepository;

@Service
public class SystemSettingsServiceImpl implements SystemSettingsService {
  private static final String CATEGORY = "system";

  private final ParamsRepository paramsRepository;

  @Autowired
  public SystemSettingsServiceImpl(ParamsRepository paramsRepository) {
    this.paramsRepository = paramsRepository;
  }

  @Override
  public SystemSettingsPojo readSettings() {
    SystemSettingsPojo target = new SystemSettingsPojo();
    for (Param param : paramsRepository.findParamsByCategory(CATEGORY)) {
      String value = param.getValue();
      switch (param.getName()) {
        case "support_email":
          target.setSupportEmail(value);
          break;
        case "support_phone":
          target.setSupportPhone(value);
          break;
        case "maintenance_mode":
          target.setMaintenanceMode(parseBoolean(value));
          break;
        default:
          break;
      }
    }
    return target;
  }

  private static boolean parseBoolean(String value) {
    if (value == null) {
      return false;
    }
    String normalized = value.trim().toLowerCase();
    return "true".equals(normalized)
      || "1".equals(normalized)
      || "yes".equals(normalized)
      || "on".equals(normalized);
  }
}
