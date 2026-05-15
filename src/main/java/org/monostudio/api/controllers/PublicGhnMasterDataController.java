package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.monostudio.shipping.ghn.GhnApiClient;
import org.monostudio.shipping.ghn.dto.GhnDistrict;
import org.monostudio.shipping.ghn.dto.GhnProvince;
import org.monostudio.shipping.ghn.dto.GhnWard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/shipping/ghn")
@Tag(name = "Public GHN Master Data")
public class PublicGhnMasterDataController {
    private final GhnApiClient ghnApiClient;

    public PublicGhnMasterDataController(GhnApiClient ghnApiClient) {
        this.ghnApiClient = ghnApiClient;
    }

    @GetMapping("/provinces")
    @Operation(summary = "List GHN provinces")
    public List<GhnProvince> listProvinces() {
        return ghnApiClient.listProvinces();
    }

    @GetMapping("/districts")
    @Operation(summary = "List GHN districts by province")
    public List<GhnDistrict> listDistricts(@RequestParam int provinceId) {
        return ghnApiClient.listDistricts(provinceId);
    }

    @GetMapping("/wards")
    @Operation(summary = "List GHN wards by district")
    public List<GhnWard> listWards(@RequestParam int districtId) {
        return ghnApiClient.listWards(districtId);
    }
}
