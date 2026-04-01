package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.DataGenericController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.OrderStatusPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.jpa.entities.OrderStatus;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.OrderStatusesCrudService;
import org.monostudio.jpa.services.predicates.OrderStatusesPredicateService;
import org.monostudio.jpa.sortspecs.OrderStatusesSortSpec;

import java.util.Map;

@RestController
@RequestMapping("/data/order_statuses")
@Tag(name = "Params management")
@PreAuthorize("isAuthenticated()")
public class DataOrderStatusesController
    extends DataGenericController<OrderStatusPojo, OrderStatus> {

    @Autowired
    public DataOrderStatusesController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        OrderStatusesCrudService crudService,
        OrderStatusesPredicateService predicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List order statuses.")
    @PreAuthorize("hasAuthority('order_statuses:read')")
    public DataPagePojo<OrderStatusPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return OrderStatusesSortSpec.ORDER_SPEC_MAP;
    }
}
