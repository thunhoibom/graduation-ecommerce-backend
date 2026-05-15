package org.monostudio.api.services;

import org.monostudio.api.models.AdminNotificationEventPojo;
import org.monostudio.api.models.OrderPojo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AdminNotificationService {
    SseEmitter subscribe(String principalKey);

    void publishOrderCreated(OrderPojo orderPojo);

    void publishHeartbeat();

    AdminNotificationEventPojo buildOrderCreatedEvent(OrderPojo orderPojo);
}
