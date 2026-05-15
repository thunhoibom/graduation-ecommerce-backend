package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.PublicPromotionSummaryPojo;
import org.monostudio.api.services.PublicPromotionCatalogService;

import java.util.List;

@RestController
@RequestMapping("/api/public/promotions")
@Tag(name = "Promotions (public)")
public class PublicPromotionsController {

    private final PublicPromotionCatalogService publicPromotionCatalogService;

    @Autowired
    public PublicPromotionsController(PublicPromotionCatalogService publicPromotionCatalogService) {
        this.publicPromotionCatalogService = publicPromotionCatalogService;
    }

    @GetMapping
    @Operation(summary = "Danh sách chương trình khuyến mãi đang hiệu lực (cho storefront)")
    public List<PublicPromotionSummaryPojo> list(@RequestParam(required = false) String productBarcode) {
        return publicPromotionCatalogService.listPublicSummaries(productBarcode);
    }
}
