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
import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.PeopleCrudService;
import org.monostudio.jpa.services.predicates.PeoplePredicateService;
import org.monostudio.jpa.sortspecs.PeopleSortSpec;

import java.util.Map;

@RestController
@RequestMapping("/api/data/people")
@Tag(name = "People management")
@PreAuthorize("isAuthenticated()")
public class DataPeopleController
    extends DataGenericController<PersonPojo, Person> {

    @Autowired
    public DataPeopleController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        PeopleCrudService crudService,
        PeoplePredicateService predicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List people.")
    @PreAuthorize("hasAuthority('people:read')")
    public DataPagePojo<PersonPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return PeopleSortSpec.ORDER_SPEC_MAP;
    }
}
