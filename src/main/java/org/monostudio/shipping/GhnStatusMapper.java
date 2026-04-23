package org.monostudio.shipping;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class GhnStatusMapper {
    public enum WorkflowAction {
        NONE,
        DELIVERY_ON_ROUTE,
        DELIVERY_FAILED,
        DELIVERY_CANCELLED,
        COMPLETED,
        RETURNED
    }

    public WorkflowAction map(String rawStatus) {
        if (StringUtils.isBlank(rawStatus)) {
            return WorkflowAction.NONE;
        }
        String normalized = rawStatus.trim().toUpperCase(Locale.ROOT);

        if (normalized.contains("RETURN")
            || normalized.equals("RETURN")
            || normalized.equals("RETURNED")) {
            return WorkflowAction.RETURNED;
        }
        if (normalized.equals("DELIVERED")
            || normalized.equals("DELIVERY_COMPLETE")
            || normalized.equals("COMPLETED")) {
            return WorkflowAction.COMPLETED;
        }
        if (normalized.contains("FAIL")
            || normalized.contains("UNDELIVERABLE")
            || normalized.equals("DELIVERY_FAIL")) {
            return WorkflowAction.DELIVERY_FAILED;
        }
        if (normalized.contains("CANCEL")
            || normalized.contains("RECALL")) {
            return WorkflowAction.DELIVERY_CANCELLED;
        }
        if (normalized.equals("IN_TRANSIT")
            || normalized.equals("ON_ROUTE")
            || normalized.equals("OUT_FOR_DELIVERY")
            || normalized.equals("DELIVERING")
            || normalized.equals("PICKING")
            || normalized.equals("MONEY_COLLECT_PICKING")
            || normalized.equals("SORTING")
            || normalized.equals("READY_TO_PICK")) {
            return WorkflowAction.DELIVERY_ON_ROUTE;
        }
        return WorkflowAction.NONE;
    }
}
