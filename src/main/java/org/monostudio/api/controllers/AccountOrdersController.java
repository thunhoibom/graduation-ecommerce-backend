package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.exceptions.UserNotFoundException;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.services.conversion.OrdersConverterService;

import jakarta.persistence.EntityNotFoundException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/account/orders")
@Tag(name = "My Orders")
@PreAuthorize("isAuthenticated()")
public class AccountOrdersController {
    private final OrdersRepository ordersRepository;
    private final OrdersConverterService ordersConverterService;
    private final CustomersRepository customersRepository;
    private final UserDetailsService userDetailsService;

    @Autowired
    public AccountOrdersController(
        OrdersRepository ordersRepository,
        OrdersConverterService ordersConverterService,
        CustomersRepository customersRepository,
        UserDetailsService userDetailsService
    ) {
        this.ordersRepository = ordersRepository;
        this.ordersConverterService = ordersConverterService;
        this.customersRepository = customersRepository;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Lists all orders belonging to the authenticated customer.
     * Returns paginated results sorted by date descending.
     *
     * @param pageIndex Page index (0-based), defaults to 0
     * @param pageSize  Items per page, defaults to configured page size
     * @param principal Authenticated user principal
     * @return Paginated list of OrderPojo
     * @throws UserNotFoundException   If the user has no associated Customer record
     * @throws EntityNotFoundException If no orders are found
     */
    @GetMapping
    @Operation(summary = "List all orders for the authenticated customer")
    public DataPagePojo<OrderPojo> listMyOrders(
        @RequestParam(required = false) Integer pageIndex,
        @RequestParam(required = false) Integer pageSize,
        Principal principal
    ) throws UserNotFoundException, EntityNotFoundException {
        Customer customer = resolveCustomer(principal);
        List<org.monostudio.jpa.entities.Order> allOrders =
            ordersRepository.findByCustomerId(customer.getId());

        if (allOrders.isEmpty()) {
            throw new EntityNotFoundException("No orders found for this account");
        }

        int idx = pageIndex != null ? pageIndex : 0;
        int size = pageSize != null ? pageSize : 20;

        int fromIndex = idx * size;
        if (fromIndex >= allOrders.size()) {
            throw new EntityNotFoundException("No orders found for this account");
        }

        int toIndex = Math.min(fromIndex + size, allOrders.size());
        List<org.monostudio.jpa.entities.Order> pageContent = allOrders.subList(fromIndex, toIndex);

        List<OrderPojo> pojos = pageContent.stream()
            .map(ordersConverterService::convertToPojo)
            .toList();

        DataPagePojo<OrderPojo> result = new DataPagePojo<>();
        result.setItems(pojos);
        result.setTotalCount(allOrders.size());
        result.setPageSize(size);
        return result;
    }

    /**
     * Retrieves a single order by its buyOrder (id) for the authenticated customer.
     * Customers can only view their own orders.
     *
     * @param buyOrder  The order ID (buyOrder)
     * @param principal Authenticated user principal
     * @return The OrderPojo with full details
     * @throws EntityNotFoundException If the order does not exist or does not belong to the customer
     */
    @GetMapping("/{buyOrder}")
    @Operation(summary = "Get a specific order by ID for the authenticated customer")
    public OrderPojo getMyOrder(@PathVariable Long buyOrder, Principal principal)
        throws EntityNotFoundException, UserNotFoundException {
        Customer customer = resolveCustomer(principal);
        org.monostudio.jpa.entities.Order order =
            ordersRepository.findByIdWithDetails(buyOrder)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Order not found: " + buyOrder));

        // Security: ensure the order belongs to the authenticated customer
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new EntityNotFoundException("Order not found: " + buyOrder);
        }

        return ordersConverterService.convertToPojo(order);
    }

    /**
     * Resolves the Customer entity from the authenticated Principal.
     *
     * @param principal The authenticated user
     * @return The Customer entity linked to the user
     * @throws UserNotFoundException If no user or customer record is found
     */
    private Customer resolveCustomer(Principal principal) throws UserNotFoundException {
        String username = principal.getName();
        User user = (User) userDetailsService.loadUserByUsername(username);
        if (user.getPerson() == null) {
            throw new UserNotFoundException("User has no associated person profile");
        }
        return customersRepository.findByPersonId(user.getPerson().getId())
            .orElseThrow(() -> new UserNotFoundException(
                "No customer record found for this user"));
    }
}
