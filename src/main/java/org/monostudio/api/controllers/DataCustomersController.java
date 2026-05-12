package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.CustomerAdminPojo;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.services.CustomerAdminViewService;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.CustomersCrudService;
import org.monostudio.jpa.services.predicates.CustomersPredicateService;
import org.monostudio.jpa.sortspecs.CustomersSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/customers")
@Tag(name = "People management")
@PreAuthorize("isAuthenticated()")
public class DataCustomersController
    extends DataCrudGenericController<PersonPojo, Customer> {

    private final CustomerAdminViewService customerAdminViewService;
    private final OrdersRepository ordersRepository;
    private final UsersRepository usersRepository;
    private final CustomersRepository customersRepository;

    @Autowired
    public DataCustomersController(
        PaginationService paginationService,
        SortSpecParserService sortSpecParserService,
        CustomersCrudService crudService,
        CustomersPredicateService predicateService,
        CustomerAdminViewService customerAdminViewService,
        OrdersRepository ordersRepository,
        UsersRepository usersRepository,
        CustomersRepository customersRepository
    ) {
        super(paginationService, sortSpecParserService, crudService, predicateService);
        this.customerAdminViewService = customerAdminViewService;
        this.ordersRepository = ordersRepository;
        this.usersRepository = usersRepository;
        this.customersRepository = customersRepository;
    }

    @Override
    @GetMapping
    @Operation(summary = "List customers.")
    @PreAuthorize("hasAuthority('customers:read')")
    public DataPagePojo<PersonPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Register new customer.")
    @PreAuthorize("hasAuthority('customers:create')")
    @ResponseStatus(CREATED)
    public void create(@RequestBody PersonPojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace customers data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('customers:update')")
    public void update(@RequestBody PersonPojo input, @PathVariable Long id)
        throws EntityNotFoundException, BadInputException {
        crudService.update(input, id);
    }

    @GetMapping("/{id}/detail")
    @Operation(summary = "Customer profile for admin (orders, addresses, aggregates).")
    @PreAuthorize("hasAuthority('customers:read')")
    public CustomerAdminPojo getAdminDetail(@PathVariable Long id) throws EntityNotFoundException {
        return customerAdminViewService.buildAdminDetail(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deregister customers.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('customers:delete')")
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException, BadInputException {
        validateCustomerDeletionAllowed(id);
        crudService.delete(id);
    }

    private void validateCustomerDeletionAllowed(Long customerId) throws BadInputException, EntityNotFoundException {
        Customer customer = customersRepository.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));
        if (ordersRepository.countForCustomer(customerId) > 0) {
            throw new BadInputException("Không thể xóa khách hàng đã có đơn hàng.");
        }
        if (usersRepository.findByPersonId(customer.getPerson().getId()).isPresent()) {
            throw new BadInputException("Không thể xóa khách đã liên kết tài khoản đăng nhập.");
        }
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partial update of customers.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('customers:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return CustomersSortSpec.ORDER_SPEC_MAP;
    }
}
