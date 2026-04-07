package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.DataGenericController;
import org.monostudio.api.models.BillingTypePojo;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.jpa.entities.BillingType;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.BillingTypesCrudService;
import org.monostudio.jpa.services.predicates.BillingTypesPredicateService;
import org.monostudio.jpa.sortspecs.BillingTypesSortSpec;

import java.util.Map;

@RestController
@RequestMapping("/api/data/billing_types")
@Tag(name = "Params management")
public class DataBillingTypesController
    extends DataGenericController<BillingTypePojo, BillingType> {

    @Autowired
    public DataBillingTypesController(
        PaginationService paginationService,
        SortSpecParserService sortSpecParserService,
        BillingTypesCrudService crudService,
        BillingTypesPredicateService predicateService
    ) {
        super(paginationService, sortSpecParserService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List billing types.")
    public DataPagePojo<BillingTypePojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return BillingTypesSortSpec.ORDER_SPEC_MAP;
    }
}
