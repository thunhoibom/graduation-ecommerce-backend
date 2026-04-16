package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.CartSessionPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.CartSession;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.CartSessionsCrudService;
import org.monostudio.jpa.services.predicates.CartSessionsPredicateService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityExistsException;
import java.util.Map;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/cart-sessions")
@Tag(name = "Cart sessions — Admin read-only")
public class DataCartSessionsController
    extends DataCrudGenericController<CartSessionPojo, CartSession> {

    @Autowired
    public DataCartSessionsController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        CartSessionsCrudService crudService,
        CartSessionsPredicateService predicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List cart sessions (admin read-only).")
    public org.monostudio.api.models.DataPagePojo<CartSessionPojo> readMany(
        @RequestParam Map<String, String> allRequestParams
    ) {
        return super.readMany(allRequestParams);
    }

    /**
     * PATCH /data/cart-sessions/{id} — Admin can refresh cart expiry (extend TTL).
     * Body: { "refreshExpiry": true, "expiresAt": "2026-04-04T00:00:00" }
     */
    @Override
    @PatchMapping("/{id}")
    @Operation(summary = "Refresh cart expiry / extend TTL.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('cartSessions:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("No element was found to update"));
    }

    /**
     * DELETE /data/cart-sessions/{id} — Admin can force-clear a cart.
     * WARNING: This does NOT release stock reservations.
     * Use DELETE /public/cart/reservations?sessionId=xxx first.
     */
    @Override
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete cart sessions (admin). Does NOT release stock reservations.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('cartSessions:delete')")
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        // No custom sort — return empty map
        return Map.of();
    }

    @Override
    public void create(CartSessionPojo input) throws BadInputException, EntityExistsException {
        throw new UnsupportedOperationException("Cart session creation is not supported via admin API");
    }

    @Override
    public void update(CartSessionPojo input, Map<String, String> requestParams)
        throws BadInputException, EntityNotFoundException {
        throw new UnsupportedOperationException("Cart session full update is not supported via admin API");
    }
}
