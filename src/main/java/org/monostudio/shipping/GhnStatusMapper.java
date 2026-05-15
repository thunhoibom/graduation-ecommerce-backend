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

        return switch (normalized) {
            case "READY_TO_PICK",
                 "PICKING",
                 "MONEY_COLLECT_PICKING",
                 "PICKED",
                 "STORING",
                 "TRANSPORTING",
                 "SORTING",
                 "DELIVERING",
                 "MONEY_COLLECT_DELIVERING" -> WorkflowAction.DELIVERY_ON_ROUTE;
            case "DELIVERED",
                 "DELIVERY_COMPLETE",
                 "COMPLETED" -> WorkflowAction.COMPLETED;
            case "RETURNED" -> WorkflowAction.RETURNED;
            case "DELIVERY_FAIL",
                 "WAITING_TO_RETURN",
                 "RETURN",
                 "RETURN_TRANSPORTING",
                 "RETURN_SORTING",
                 "RETURNING",
                 "RETURN_FAIL" -> WorkflowAction.NONE;
            case "EXCEPTION",
                 "DAMAGE",
                 "LOST" -> WorkflowAction.DELIVERY_FAILED;
            default -> mapLegacyAlias(normalized);
        };
    }

    private WorkflowAction mapLegacyAlias(String normalized) {
        if (normalized.contains("UNDELIVERABLE")) {
            return WorkflowAction.DELIVERY_FAILED;
        }
        if (normalized.contains("CANCEL") || normalized.contains("RECALL")) {
            return WorkflowAction.DELIVERY_CANCELLED;
        }
        if (normalized.equals("IN_TRANSIT")
            || normalized.equals("ON_ROUTE")
            || normalized.equals("OUT_FOR_DELIVERY")) {
            return WorkflowAction.DELIVERY_ON_ROUTE;
        }
        return WorkflowAction.NONE;
    }
}
