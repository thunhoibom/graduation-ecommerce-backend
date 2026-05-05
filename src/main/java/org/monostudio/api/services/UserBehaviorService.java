package org.monostudio.api.services;

import org.monostudio.api.models.BehaviorEventRequestPojo;

import java.util.List;

public interface UserBehaviorService {

    void recordBestEffort(BehaviorEventRequestPojo request);

    List<String> rankCategoryCodesForDevice(String deviceId, int maxCategories);
}
