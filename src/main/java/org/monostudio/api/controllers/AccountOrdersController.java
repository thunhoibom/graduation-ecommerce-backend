package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.monostudio.jpa.repositories.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

import org.monostudio.jpa.repositories.PeopleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/account/orders")
@Tag(name = "My Orders")
@PreAuthorize("isAuthenticated()")
public class AccountOrdersController {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_READY_TO_PICK = "READY_TO_PICK";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_CANCELLATION_REQUESTED = "CANCELLATION_REQUESTED";
    private static final String PAYMENT_UNPAID = "UNPAID";
    private static final String PAYMENT_EXPIRED = "EXPIRED";
    private static final String PAYMENT_CANCELLED = "PAYMENT_CANCELLED";
    private static final Set<String> FREE_CANCEL_FULFILLMENT = Set.of(STATUS_PENDING, STATUS_CONFIRMED);
    private static final Set<String> REQUEST_CANCEL_FULFILLMENT = Set.of(STATUS_PROCESSING, STATUS_READY_TO_PICK);
    private static final Set<String> FREE_CANCEL_PAYMENT = Set.of(PAYMENT_UNPAID, PAYMENT_EXPIRED, PAYMENT_CANCELLED);

    private final OrdersRepository ordersRepository;
    private final OrdersConverterService ordersConverterService;
    private final CustomersRepository customersRepository;
    private final UsersRepository usersRepository;
    private final PeopleRepository peopleRepository;
    private final UserDetailsService userDetailsService;

    @Autowired
    public AccountOrdersController(
        OrdersRepository ordersRepository,
        OrdersConverterService ordersConverterService,
        CustomersRepository customersRepository,
        UsersRepository usersRepository,
        PeopleRepository peopleRepository,
        UserDetailsService userDetailsService
    ) {
        this.ordersRepository = ordersRepository;
        this.ordersConverterService = ordersConverterService;
        this.customersRepository = customersRepository;
        this.usersRepository = usersRepository;
        this.peopleRepository = peopleRepository;
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
        String email = customer.getPerson().getEmail();
        
        // Find all orders linked to this customer OR any customer sharing the same person email
        List<org.monostudio.jpa.entities.Order> allOrders =
            ordersRepository.findByCustomerPersonEmail(email);

        int idx = pageIndex != null ? pageIndex : 0;
        int size = pageSize != null ? pageSize : 20;

        DataPagePojo<OrderPojo> result = new DataPagePojo<>();
        result.setPageSize(size);

        if (allOrders.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(0);
            return result;
        }

        int fromIndex = idx * size;
        if (fromIndex >= allOrders.size()) {
            result.setItems(List.of());
            result.setTotalCount(allOrders.size());
            return result;
        }

        int toIndex = Math.min(fromIndex + size, allOrders.size());
        List<org.monostudio.jpa.entities.Order> pageContent = allOrders.subList(fromIndex, toIndex);

        List<OrderPojo> pojos = pageContent.stream()
            .map(ordersConverterService::convertToPojo)
            .toList();

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

        // Security: ensure the order belongs to the authenticated customer.
        // We use email comparison to be consistent with listMyOrders, 
        // since a single person might have multiple customer IDs (e.g. from guest orders).
        String userEmail = customer.getPerson().getEmail();
        String orderEmail = order.getCustomer().getPerson().getEmail();

        if (userEmail == null || !userEmail.equalsIgnoreCase(orderEmail)) {
            throw new EntityNotFoundException("Order not found: " + buyOrder);
        }

        return ordersConverterService.convertToPojo(order);
    }

    @PostMapping("/{buyOrder}/cancel")
    @Operation(summary = "Cancel order by customer policy (free cancel / cancellation request / locked)")
    public OrderPojo cancelMyOrder(@PathVariable Long buyOrder, Principal principal)
        throws EntityNotFoundException, UserNotFoundException {
        Customer customer = resolveCustomer(principal);
        org.monostudio.jpa.entities.Order order =
            ordersRepository.findByIdWithDetails(buyOrder)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + buyOrder));

        String userEmail = customer.getPerson().getEmail();
        String orderEmail = order.getCustomer().getPerson().getEmail();
        if (userEmail == null || !userEmail.equalsIgnoreCase(orderEmail)) {
            throw new EntityNotFoundException("Order not found: " + buyOrder);
        }

        String fulfillment = normalizeFulfillment(order.getFulfillmentStatus());
        String payment = normalizePayment(order.getPaymentStatus());

        if (FREE_CANCEL_FULFILLMENT.contains(fulfillment) && FREE_CANCEL_PAYMENT.contains(payment)) {
            ordersRepository.setFulfillmentStatus(order.getId(), STATUS_CANCELLED);
        } else if (REQUEST_CANCEL_FULFILLMENT.contains(fulfillment)) {
            ordersRepository.setFulfillmentStatus(order.getId(), STATUS_CANCELLATION_REQUESTED);
        } else {
            throw new jakarta.persistence.EntityNotFoundException(
                "Order cannot be cancelled at fulfillment status: " + fulfillment
            );
        }

        org.monostudio.jpa.entities.Order refreshed =
            ordersRepository.findByIdWithDetails(buyOrder)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + buyOrder));
        return ordersConverterService.convertToPojo(refreshed);
    }

    private String normalizeFulfillment(String status) {
        if (status == null) {
            return STATUS_PENDING;
        }
        return switch (status.toUpperCase()) {
            case "DELIVERY_ON_ROUTE" -> "DELIVERING";
            case "DELIVERY_COMPLETE" -> "DELIVERED";
            case "DELIVERY_CANCELLED" -> STATUS_CANCELLED;
            default -> status.toUpperCase();
        };
    }

    private String normalizePayment(String status) {
        if (status == null) {
            return PAYMENT_UNPAID;
        }
        return switch (status.toUpperCase()) {
            case PAYMENT_CANCELLED -> PAYMENT_EXPIRED;
            default -> status.toUpperCase();
        };
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
        User user = usersRepository.findByNameWithProfile(username)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        if (user.getPerson() == null) {
            throw new UserNotFoundException("User has no associated person profile");
        }
        return customersRepository.findByPersonId(user.getPerson().getId())
            .orElseThrow(() -> new UserNotFoundException(
                "No customer record found for this user"));
    }
}
