package org.monostudio.api.services;

import org.monostudio.api.models.PublicPromotionSummaryPojo;

import java.util.List;

public interface PublicPromotionCatalogService {

    /**
     * @param productBarcode optional — khi có, luôn hiển thị rule không giới hạn SP;
     *                       rule chỉ áp dụng {@code cart.has_any_product} được giữ nếu barcode nằm trong danh sách.
     */
    List<PublicPromotionSummaryPojo> listPublicSummaries(String productBarcode);
}
