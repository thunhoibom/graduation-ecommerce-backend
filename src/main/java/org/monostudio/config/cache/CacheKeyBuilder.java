package org.monostudio.config.cache;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Component("cacheKeyBuilder")
public class CacheKeyBuilder {

    public String fromParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "no_params";
        }
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(entry.getKey())
                .append("=")
                .append(Objects.toString(entry.getValue(), ""));
        }
        return builder.toString();
    }

    public String shipping(Integer subtotal, Double latitude, Double longitude) {
        return "subtotal=" + Objects.toString(subtotal, "null")
            + "|lat=" + normalizeCoordinate(latitude)
            + "|lon=" + normalizeCoordinate(longitude);
    }

    public String discountValidation(String code, int subtotal) {
        return Objects.toString(code, "").trim().toLowerCase() + "|" + subtotal;
    }

    public String dashboard(Object from, Object to) {
        return "from=" + Objects.toString(from, "null") + "|to=" + Objects.toString(to, "null");
    }

    public String dashboardRevenue(Object from, Object to, String groupBy) {
        return dashboard(from, to) + "|groupBy=" + Objects.toString(groupBy, "day");
    }

    public String dashboardTopProducts(Object from, Object to, int limit) {
        return dashboard(from, to) + "|limit=" + limit;
    }

    public String fromIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "no_ids";
        }
        return ids.stream()
            .sorted()
            .map(String::valueOf)
            .reduce((left, right) -> left + "," + right)
            .orElse("no_ids");
    }

    private String normalizeCoordinate(Double value) {
        if (value == null) {
            return "null";
        }
        return String.format(java.util.Locale.ROOT, "%.6f", value);
    }
}
