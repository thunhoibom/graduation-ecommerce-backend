package org.monostudio.shipping.ghn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.monostudio.shipping.ghn.dto.GhnCreateOrderRequest;
import org.monostudio.shipping.ghn.dto.GhnCreateOrderResult;
import org.monostudio.shipping.ghn.dto.GhnAvailableService;
import org.monostudio.shipping.ghn.dto.GhnCreateOrderItem;
import org.monostudio.shipping.ghn.dto.GhnDistrict;
import org.monostudio.shipping.ghn.dto.GhnProvince;
import org.monostudio.shipping.ghn.dto.GhnRateRequest;
import org.monostudio.shipping.ghn.dto.GhnRateResult;
import org.monostudio.shipping.ghn.dto.GhnWard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GhnApiClient {
    private static final Logger logger = LoggerFactory.getLogger(GhnApiClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final GhnConfig config;
    private final HttpClient httpClient;

    public GhnApiClient(GhnConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Math.max(config.getTimeoutMs(), 1000)))
            .build();
    }

    public GhnRateResult quoteFee(GhnRateRequest request) {
        ensureEnabled();
        Map<String, Object> payload = new HashMap<>();
        payload.put("shop_id", request.getShopId());
        if (request.getServiceId() != null && request.getServiceId() > 0) {
            payload.put("service_id", request.getServiceId());
        }
        if (request.getServiceTypeId() != null && request.getServiceTypeId() > 0) {
            payload.put("service_type_id", request.getServiceTypeId());
        }
        payload.put("insurance_value", request.getInsuranceValue());
        payload.put("from_district_id", request.getFromDistrictId());
        payload.put("from_ward_code", request.getFromWardCode());
        payload.put("to_district_id", request.getToDistrictId());
        payload.put("to_ward_code", request.getToWardCode());
        payload.put("weight", request.getWeight());
        payload.put("length", request.getLength());
        payload.put("width", request.getWidth());
        payload.put("height", request.getHeight());
        logger.info("GHN quote-fee request: shopId={}, fromDistrict={}, toDistrict={}, toWard={}, serviceId={}, serviceTypeId={}",
            request.getShopId(), request.getFromDistrictId(), request.getToDistrictId(), request.getToWardCode(),
            request.getServiceId(), request.getServiceTypeId());

        JsonNode data = post(config.getRatePath(), payload);
        int totalFee = extractIntByPriority(data, "total", "total_fee", "service_fee");
        return GhnRateResult.builder()
            .totalFee(totalFee)
            .source("GHN_LIVE")
            .build();
    }

    public List<GhnProvince> listProvinces() {
        ensureEnabled();
        JsonNode data = get("/master-data/province");
        List<GhnProvince> provinces = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                provinces.add(GhnProvince.builder()
                    .provinceId(item.path("ProvinceID").asInt())
                    .provinceName(item.path("ProvinceName").asText(""))
                    .build());
            }
        }
        return provinces;
    }

    public List<GhnDistrict> listDistricts(int provinceId) {
        ensureEnabled();
        Map<String, Object> payload = new HashMap<>();
        payload.put("province_id", provinceId);
        JsonNode data = post("/master-data/district", payload);
        List<GhnDistrict> districts = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                districts.add(GhnDistrict.builder()
                    .districtId(item.path("DistrictID").asInt())
                    .provinceId(item.path("ProvinceID").asInt())
                    .districtName(item.path("DistrictName").asText(""))
                    .build());
            }
        }
        return districts;
    }

    public List<GhnWard> listWards(int districtId) {
        ensureEnabled();
        Map<String, Object> payload = new HashMap<>();
        payload.put("district_id", districtId);
        JsonNode data = post("/master-data/ward", payload);
        List<GhnWard> wards = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                wards.add(GhnWard.builder()
                    .wardCode(item.path("WardCode").asText(""))
                    .districtId(item.path("DistrictID").asInt())
                    .wardName(item.path("WardName").asText(""))
                    .build());
            }
        }
        return wards;
    }

    public GhnCreateOrderResult createOrder(GhnCreateOrderRequest request) {
        ensureEnabled();
        Map<String, Object> payload = new HashMap<>();
        payload.put("client_order_code", String.valueOf(request.getOrderCode()));
        payload.put("shop_id", request.getShopId());
        payload.put("service_id", request.getServiceId());
        if (request.getServiceTypeId() != null && request.getServiceTypeId() > 0) {
            payload.put("service_type_id", request.getServiceTypeId());
        }
        payload.put("payment_type_id", request.getPaymentTypeId());
        payload.put("note", request.getNote());
        payload.put("cod_amount", request.getCodAmount());
        payload.put("cod_failed_amount", request.getCodFailedAmount());
        payload.put("insurance_value", request.getInsuranceValue());
        payload.put("return_phone", request.getReturnPhone());
        payload.put("return_address", request.getReturnAddress());
        payload.put("return_district_id", request.getReturnDistrictId());
        payload.put("return_ward_code", request.getReturnWardCode());
        payload.put("from_name", request.getFromName());
        payload.put("from_phone", request.getFromPhone());
        payload.put("from_address", request.getFromAddress());
        payload.put("from_ward_name", request.getFromWardName());
        payload.put("from_district_name", request.getFromDistrictName());
        payload.put("from_province_name", request.getFromProvinceName());
        payload.put("required_note", request.getRequiredNote());
        payload.put("content", StringUtils.defaultIfBlank(request.getContent(), config.getDefaultContent()));
        payload.put("to_name", request.getToName());
        payload.put("to_phone", request.getToPhone());
        payload.put("to_address", request.getToAddress());
        payload.put("to_ward_name", request.getToWardName());
        payload.put("to_district_name", request.getToDistrictName());
        payload.put("to_province_name", request.getToProvinceName());
        payload.put("to_ward_code", request.getToWardCode());
        payload.put("to_district_id", request.getToDistrictId());
        payload.put("pick_station_id", request.getPickStationId());
        payload.put("deliver_station_id", request.getDeliverStationId());
        payload.put("coupon", request.getCoupon());
        payload.put("pickup_time", request.getPickupTime());
        payload.put("pick_shift", request.getPickShift());
        payload.put("weight", request.getWeight());
        payload.put("length", request.getLength());
        payload.put("width", request.getWidth());
        payload.put("height", request.getHeight());
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (GhnCreateOrderItem item : request.getItems()) {
                Map<String, Object> mapped = new HashMap<>();
                mapped.put("name", item.getName());
                mapped.put("code", item.getCode());
                mapped.put("quantity", item.getQuantity());
                mapped.put("price", item.getPrice());
                mapped.put("length", item.getLength());
                mapped.put("width", item.getWidth());
                mapped.put("height", item.getHeight());
                mapped.put("weight", item.getWeight());
                items.add(mapped);
            }
            payload.put("items", items);
        }
        logger.info("GHN create-order request: orderCode={}, shopId={}, toDistrict={}, toWard={}, serviceId={}, serviceTypeId={}, paymentTypeId={}, codAmount={}",
            request.getOrderCode(), request.getShopId(), request.getToDistrictId(), request.getToWardCode(),
            request.getServiceId(), request.getServiceTypeId(), request.getPaymentTypeId(), request.getCodAmount());

        JsonNode data = post(config.getCreateOrderPath(), payload);
        String trackingNumber = extractTextByPriority(data, "tracking_number", "order_code");
        String orderCode = extractTextByPriority(data, "order_code", "client_order_code");
        String sortCode = extractTextByPriority(data, "sort_code");
        Integer totalFee = parseNullableInt(data.path("total_fee"));
        Instant expectedDeliveryTime = parseNullableInstant(data.path("expected_delivery_time"));

        if (StringUtils.isBlank(trackingNumber)) {
            throw new GhnApiException("GHN create-order response missing tracking number");
        }
        return GhnCreateOrderResult.builder()
            .orderCode(orderCode)
            .trackingNumber(trackingNumber)
            .sortCode(sortCode)
            .totalFee(totalFee)
            .expectedDeliveryTime(expectedDeliveryTime)
            .build();
    }

    public int resolveServiceId(
        long shopId,
        int fromDistrictId,
        int toDistrictId,
        Integer preferredServiceId,
        Integer preferredServiceTypeId
    ) {
        ensureEnabled();
        Map<String, Object> payload = new HashMap<>();
        payload.put("shop_id", shopId);
        payload.put("from_district", fromDistrictId);
        payload.put("to_district", toDistrictId);
        logger.info("GHN available-services request: shopId={}, fromDistrict={}, toDistrict={}, preferredServiceId={}, preferredServiceTypeId={}",
            shopId, fromDistrictId, toDistrictId, preferredServiceId, preferredServiceTypeId);

        JsonNode data = post("/v2/shipping-order/available-services", payload);
        List<GhnAvailableService> services = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                Integer serviceId = parseNullableInt(item.path("service_id"));
                Integer serviceTypeId = parseNullableInt(item.path("service_type_id"));
                String shortName = item.path("short_name").asText(null);
                services.add(GhnAvailableService.builder()
                    .serviceId(serviceId)
                    .serviceTypeId(serviceTypeId)
                    .shortName(shortName)
                    .build());
            }
        }

        if (services.isEmpty()) {
            throw new GhnApiException("No GHN service available for route fromDistrict="
                + fromDistrictId + " toDistrict=" + toDistrictId);
        }

        if (preferredServiceId != null && preferredServiceId > 0) {
            Optional<GhnAvailableService> preferred = services.stream()
                .filter(s -> s.getServiceId() != null && s.getServiceId().equals(preferredServiceId))
                .findFirst();
            if (preferred.isPresent()) {
                logger.info("GHN available-services selected preferred serviceId={}", preferredServiceId);
                return preferred.get().getServiceId();
            }
        }

        if (preferredServiceTypeId != null && preferredServiceTypeId > 0) {
            Optional<GhnAvailableService> byType = services.stream()
                .filter(s -> s.getServiceTypeId() != null && s.getServiceTypeId().equals(preferredServiceTypeId))
                .filter(s -> s.getServiceId() != null && s.getServiceId() > 0)
                .findFirst();
            if (byType.isPresent()) {
                logger.info("GHN available-services selected by serviceTypeId={}, serviceId={}",
                    preferredServiceTypeId, byType.get().getServiceId());
                return byType.get().getServiceId();
            }
        }

        Optional<GhnAvailableService> firstWithServiceId = services.stream()
            .filter(s -> s.getServiceId() != null && s.getServiceId() > 0)
            .findFirst();
        if (firstWithServiceId.isEmpty()) {
            throw new GhnApiException("GHN available-services returned no usable service_id for route fromDistrict="
                + fromDistrictId + " toDistrict=" + toDistrictId);
        }
        logger.info("GHN available-services selected fallback serviceId={}", firstWithServiceId.get().getServiceId());
        return firstWithServiceId.get().getServiceId();
    }

    private JsonNode post(String path, Map<String, Object> payload) {
        ensureToken();
        try {
            String body = OBJECT_MAPPER.writeValueAsString(payload);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(joinUrl(config.getBaseUrl(), path)))
                .timeout(Duration.ofMillis(Math.max(config.getTimeoutMs(), 1000)))
                .header("Content-Type", "application/json")
                .header("Token", config.getToken())
                .POST(HttpRequest.BodyPublishers.ofString(body));
            long shopId = extractShopIdFromPayload(payload);
            if (shopId > 0) {
                requestBuilder.header("ShopId", String.valueOf(shopId));
            }
            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.error("GHN API HTTP error: path={}, status={}, payload={}", path, response.statusCode(),
                    StringUtils.abbreviate(body, 500));
                logger.error("GHN API HTTP error response body: {}", StringUtils.abbreviate(response.body(), 500));
                throw new GhnApiException("GHN API returned HTTP " + response.statusCode()
                    + " body=" + StringUtils.abbreviate(response.body(), 300));
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            return unwrapData(root);
        } catch (GhnApiException e) {
            throw e;
        } catch (Exception e) {
            logger.error("GHN API call failed", e);
            throw new GhnApiException("Could not call GHN API", e);
        }
    }

    private JsonNode get(String path) {
        ensureToken();
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(joinUrl(config.getBaseUrl(), path)))
                .timeout(Duration.ofMillis(Math.max(config.getTimeoutMs(), 1000)))
                .header("Content-Type", "application/json")
                .header("Token", config.getToken())
                .GET();
            if (config.getDefaultShopId() > 0) {
                requestBuilder.header("ShopId", String.valueOf(config.getDefaultShopId()));
            }
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new GhnApiException("GHN API returned HTTP " + response.statusCode()
                    + " body=" + StringUtils.abbreviate(response.body(), 300));
            }
            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            return unwrapData(root);
        } catch (GhnApiException e) {
            throw e;
        } catch (Exception e) {
            logger.error("GHN API call failed", e);
            throw new GhnApiException("Could not call GHN API", e);
        }
    }

    private JsonNode unwrapData(JsonNode root) {
        int code = root.path("code").asInt(200);
        if (code != 200) {
            throw new GhnApiException("GHN API business error code=" + code
                + ", message=" + root.path("message").asText("unknown"));
        }
        return root.path("data");
    }

    private String joinUrl(String base, String path) {
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }
        if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }
        return base + path;
    }

    private int extractIntByPriority(JsonNode data, String... fields) {
        for (String field : fields) {
            JsonNode node = data.path(field);
            if (node.isInt() || node.isLong() || node.isNumber()) {
                return node.asInt();
            }
        }
        return 0;
    }

    private String extractTextByPriority(JsonNode data, String... fields) {
        for (String field : fields) {
            String value = data.path(field).asText(null);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private Integer parseNullableInt(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isInt() || node.isLong() || node.isNumber()) {
            return node.asInt();
        }
        if (node.isTextual()) {
            String text = node.asText().trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Instant parseNullableInstant(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String raw = node.asText(null);
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private long extractShopIdFromPayload(Map<String, Object> payload) {
        if (payload != null) {
            Object raw = payload.get("shop_id");
            if (raw instanceof Number number && number.longValue() > 0) {
                return number.longValue();
            }
            if (raw instanceof String text && StringUtils.isNotBlank(text)) {
                try {
                    return Long.parseLong(text.trim());
                } catch (NumberFormatException ignored) {
                    // Ignore invalid shop_id string and fall back to config default.
                }
            }
        }
        return config.getDefaultShopId();
    }

    private void ensureEnabled() {
        if (!config.isEnabled()) {
            throw new GhnApiException("GHN integration is disabled");
        }
    }

    private void ensureToken() {
        if (StringUtils.isBlank(config.getToken())) {
            throw new GhnApiException("Missing GHN token configuration");
        }
    }
}
