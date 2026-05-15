package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.ReturnRequestPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.api.services.ReturnRequestService;
import org.monostudio.api.services.ReturnShipmentService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ReturnRequest;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.ReturnRequestsCrudService;
import org.monostudio.jpa.services.predicates.ReturnRequestsPredicateService;
import org.monostudio.jpa.sortspecs.ReturnRequestsSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/return-requests")
@Tag(name = "Return Requests management")
@PreAuthorize("isAuthenticated()")
public class DataReturnRequestsController
    extends DataCrudGenericController<ReturnRequestPojo, ReturnRequest> {
    private final ReturnRequestService returnRequestService;
    private final ReturnShipmentService returnShipmentService;

    @Autowired
    public DataReturnRequestsController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        ReturnRequestsCrudService crudService,
        ReturnRequestsPredicateService predicateService,
        ReturnRequestService returnRequestService,
        ReturnShipmentService returnShipmentService
    ) {
        super(paginationService, sortService, crudService, predicateService);
        this.returnRequestService = returnRequestService;
        this.returnShipmentService = returnShipmentService;
    }

    @Override
    @GetMapping
    @Operation(summary = "List return requests.")
    @PreAuthorize("hasAuthority('returnRequests:read')")
    public DataPagePojo<ReturnRequestPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        if (allRequestParams != null) {
            if (allRequestParams.containsKey("id")) {
                Predicate predicate = predicateService.parseMap(allRequestParams);
                ReturnRequestPojo returnRequest = crudService.readOne(predicate);
                DataPagePojo<ReturnRequestPojo> singleItemPage = new DataPagePojo<>();
                singleItemPage.setItems(List.of(returnRequest));
                singleItemPage.setTotalCount(1);
                singleItemPage.setPageSize(1);
                return singleItemPage;
            }
            if (!allRequestParams.containsKey("sortBy") && !allRequestParams.containsKey("order")) {
                allRequestParams = new HashMap<>(allRequestParams);
                allRequestParams.put("sortBy", "id");
                allRequestParams.put("order", "desc");
            }
        }
        DataPagePojo<ReturnRequestPojo> page = super.readMany(allRequestParams);
        if (page.getItems() != null && !page.getItems().isEmpty()) {
            List<ReturnRequestPojo> enriched = page.getItems().stream()
                .map(item -> {
                    if (item.getId() == null) {
                        return item;
                    }
                    try {
                        return crudService.findById(item.getId());
                    } catch (EntityNotFoundException ignored) {
                        return item;
                    }
                })
                .collect(Collectors.toList());
            page.setItems(enriched);
        }
        return page;
    }

    @Override
    @PostMapping
    @Operation(summary = "Create a new return request.")
    @ResponseStatus(CREATED)
    @PreAuthorize("isAuthenticated()")
    public void create(@RequestBody ReturnRequestPojo input)
        throws BadInputException, EntityExistsException {
        returnRequestService.createReturnRequest(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace return request data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('returnRequests:update')")
    public void update(ReturnRequestPojo input, @PathVariable Long id)
        throws BadInputException, EntityNotFoundException {
        crudService.update(input, id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update parts of return request data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('returnRequests:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("No element was found to update"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove return requests.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('returnRequests:delete')")
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
    }

    @PostMapping("/approve/{id}")
    @Operation(summary = "Approve a return request and release reserved stock.")
    @PreAuthorize("hasAuthority('returnRequests:update')")
    public ReturnRequestPojo approveReturnRequest(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, Object> body
    ) throws EntityNotFoundException, BadInputException {
        String adminNotes = body != null ? (String) body.get("adminNotes") : null;
        Integer refundAmount = null;
        if (body != null && body.get("refundAmount") != null) {
            refundAmount = ((Number) body.get("refundAmount")).intValue();
        }
        return returnRequestService.approveReturnRequest(id, adminNotes, refundAmount);
    }

    @PostMapping("/reject/{id}")
    @Operation(summary = "Reject a return request.")
    @PreAuthorize("hasAuthority('returnRequests:update')")
    public ReturnRequestPojo rejectReturnRequest(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, Object> body
    ) throws EntityNotFoundException, BadInputException {
        String adminNotes = body != null ? (String) body.get("adminNotes") : null;
        return returnRequestService.rejectReturnRequest(id, adminNotes);
    }

    @PostMapping("/receive/{id}")
    @Operation(summary = "Mark returned items as received at warehouse.")
    @PreAuthorize("hasAuthority('returnRequests:update')")
    public ReturnRequestPojo markAsReceived(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, Object> body
    ) throws EntityNotFoundException, BadInputException {
        String adminNotes = body != null ? (String) body.get("adminNotes") : null;
        return returnRequestService.markAsReceived(id, adminNotes);
    }

    @PostMapping("/qc/{id}")
    @Operation(summary = "Record warehouse QC for a received return request.")
    @PreAuthorize("hasAuthority('returnRequests:update')")
    public ReturnRequestPojo submitQc(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, Object> body
    ) throws EntityNotFoundException, BadInputException {
        String result = body != null ? (String) body.get("result") : null;
        String qcNotes = body != null ? (String) body.get("qcNotes") : null;
        String qcPhotoUrls = null;
        if (body != null && body.get("photoUrls") instanceof java.util.List<?> photoUrls) {
            qcPhotoUrls = photoUrls.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.joining(","));
        } else if (body != null && body.get("qcPhotoUrls") instanceof String photoUrlsText) {
            qcPhotoUrls = photoUrlsText;
        }
        return returnRequestService.submitQc(id, result, qcNotes, qcPhotoUrls);
    }

    @PostMapping("/complete-refund/{id}")
    @Operation(summary = "Mark refund as completed.")
    @PreAuthorize("hasAuthority('returnRequests:update')")
    public ReturnRequestPojo completeRefund(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, Object> body
    ) throws EntityNotFoundException, BadInputException {
        String adminNotes = body != null ? (String) body.get("adminNotes") : null;
        String refundProofUrl = body != null ? (String) body.get("refundProofUrl") : null;
        String refundReference = body != null ? (String) body.get("refundReference") : null;
        Instant refundedAt = null;
        if (body != null && body.get("refundedAt") instanceof String refundedAtText && !refundedAtText.isBlank()) {
            refundedAt = Instant.parse(refundedAtText.trim());
        }
        return returnRequestService.completeRefund(id, adminNotes, refundProofUrl, refundReference, refundedAt);
    }

    @PostMapping("/tracking/{id}")
    @Operation(summary = "Add a tracking number to a return request.")
    @PreAuthorize("hasAuthority('returnRequests:update')")
    public ReturnRequestPojo addTrackingNumber(
        @PathVariable Long id,
        @RequestBody Map<String, String> body
    ) throws EntityNotFoundException, BadInputException {
        String trackingNumber = body.get("trackingNumber");
        return returnRequestService.addTrackingNumber(id, trackingNumber);
    }

    @PostMapping("/cancel/{id}")
    @Operation(summary = "Cancel a pending return request.")
    @PreAuthorize("hasAuthority('returnRequests:update')")
    public ReturnRequestPojo cancelReturnRequest(@PathVariable Long id)
        throws EntityNotFoundException, BadInputException {
        return returnRequestService.cancelReturnRequest(id);
    }

    @PostMapping("/start-refund/{id}")
    @Operation(summary = "Start the refund process for a return request.")
    @PreAuthorize("hasAuthority('returnRequests:update')")
    public ReturnRequestPojo startRefund(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, Object> body
    ) throws EntityNotFoundException, BadInputException {
        String adminNotes = body != null ? (String) body.get("adminNotes") : null;
        return returnRequestService.startRefund(id, adminNotes);
    }

    @PostMapping("/notes/{id}")
    @Operation(summary = "Add a note to a return request.")
    @PreAuthorize("hasAuthority('returnRequests:update')")
    public ReturnRequestPojo addNote(
        @PathVariable Long id,
        @RequestBody Map<String, String> body
    ) throws EntityNotFoundException, BadInputException {
        String note = body.get("note");
        return returnRequestService.addNote(id, note);
    }

    @PostMapping("/retry-shipment/{id}")
    @Operation(summary = "Manually retry GHN return-shipment dispatch for a return request.")
    @PreAuthorize("hasAuthority('returnRequests:update')")
    public ReturnRequestPojo retryShipment(@PathVariable Long id)
        throws EntityNotFoundException {
        returnShipmentService.retryReturnShipmentCreation(id);
        return crudService.findById(id);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return ReturnRequestsSortSpec.ORDER_SPEC_MAP;
    }
}
