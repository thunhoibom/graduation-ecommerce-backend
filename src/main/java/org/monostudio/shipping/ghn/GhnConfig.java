package org.monostudio.shipping.ghn;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class GhnConfig {
    @Value("${monostudio.shipping.ghn.enabled:false}")
    private boolean enabled;

    @Value("${monostudio.shipping.ghn.base-url:https://dev-online-gateway.ghn.vn/shiip/public-api}")
    private String baseUrl;

    @Value("${monostudio.shipping.ghn.token:}")
    private String token;

    @Value("${monostudio.shipping.ghn.shop-id:0}")
    private long defaultShopId;

    @Value("${monostudio.shipping.ghn.from-district-id:0}")
    private int fromDistrictId;

    @Value("${monostudio.shipping.ghn.from-ward-code:}")
    private String fromWardCode;

    @Value("${monostudio.shipping.ghn.default-service-type-id:2}")
    private int defaultServiceTypeId;

    @Value("${monostudio.shipping.ghn.rate-path:/v2/shipping-order/fee}")
    private String ratePath;

    @Value("${monostudio.shipping.ghn.create-order-path:/v2/shipping-order/create}")
    private String createOrderPath;

    @Value("${monostudio.shipping.ghn.timeout-ms:5000}")
    private int timeoutMs;

    @Value("${monostudio.shipping.ghn.default-content:Mono Studio order}")
    private String defaultContent;

    @Value("${monostudio.shipping.ghn.from-name:Mono Studio}")
    private String fromName;

    @Value("${monostudio.shipping.ghn.from-phone:}")
    private String fromPhone;

    @Value("${monostudio.shipping.ghn.from-address:}")
    private String fromAddress;

    @Value("${monostudio.shipping.ghn.from-ward-name:}")
    private String fromWardName;

    @Value("${monostudio.shipping.ghn.from-district-name:}")
    private String fromDistrictName;

    @Value("${monostudio.shipping.ghn.from-province-name:}")
    private String fromProvinceName;

    @Value("${monostudio.shipping.ghn.return-phone:}")
    private String returnPhone;

    @Value("${monostudio.shipping.ghn.return-address:}")
    private String returnAddress;
}
