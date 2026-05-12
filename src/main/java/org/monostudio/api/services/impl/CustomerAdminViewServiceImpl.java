package org.monostudio.api.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.CustomerAdminPojo;
import org.monostudio.api.services.CustomerAdminViewService;
import org.monostudio.jpa.entities.Address;
import org.monostudio.jpa.entities.AddressBook;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.repositories.AddressBookRepository;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.UsersRepository;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class CustomerAdminViewServiceImpl
    implements CustomerAdminViewService {

    private final CustomersRepository customersRepository;
    private final OrdersRepository ordersRepository;
    private final UsersRepository usersRepository;
    private final AddressBookRepository addressBookRepository;

    public CustomerAdminViewServiceImpl(
        CustomersRepository customersRepository,
        OrdersRepository ordersRepository,
        UsersRepository usersRepository,
        AddressBookRepository addressBookRepository
    ) {
        this.customersRepository = customersRepository;
        this.ordersRepository = ordersRepository;
        this.usersRepository = usersRepository;
        this.addressBookRepository = addressBookRepository;
    }

    @Override
    public CustomerAdminPojo buildAdminDetail(Long customerId) throws EntityNotFoundException {
        Customer customer = customersRepository.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));
        Person person = customer.getPerson();

        List<Order> orders = ordersRepository.findByCustomerId(customerId);
        long totalSpent = orders.stream().mapToLong(Order::getTotalValue).sum();

        List<CustomerAdminPojo.CustomerOrderSummaryPojo> orderRows = orders.stream()
            .map(this::toOrderSummary)
            .collect(Collectors.toList());

        List<CustomerAdminPojo.CustomerShippingAddressPojo> addressRows = usersRepository
            .findByPersonId(person.getId())
            .map(u -> addressBookRepository.findByUserIdWithAddress(u.getId()).stream()
                .map(ab -> toShippingAddress(ab, person))
                .collect(Collectors.toList()))
            .orElse(List.of());

        return CustomerAdminPojo.builder()
            .id(customer.getId())
            .firstName(person.getFirstName())
            .lastName(person.getLastName())
            .email(person.getEmail())
            .phone1(person.getPhone1())
            .phone2(person.getPhone2())
            .idNumber(person.getIdNumber())
            .orders(orderRows)
            .orderCount(orders.size())
            .totalSpent(totalSpent)
            .addresses(addressRows)
            .build();
    }

    private CustomerAdminPojo.CustomerOrderSummaryPojo toOrderSummary(Order o) {
        Person cp = o.getCustomer().getPerson();
        String fullName = String.format("%s %s", cp.getFirstName(), cp.getLastName()).trim();
        String shipStr = null;
        if (o.getShippingAddress() != null) {
            shipStr = formatAddressOneLine(o.getShippingAddress());
        }
        return CustomerAdminPojo.CustomerOrderSummaryPojo.builder()
            .id(o.getId())
            .date(o.getDate() != null ? o.getDate().toString() : null)
            .totalValue(o.getTotalValue())
            .status(o.getFulfillmentStatus())
            .paymentStatus(o.getPaymentStatus())
            .itemCount(o.getTotalItems())
            .recipientName(fullName)
            .recipientPhone(cp.getPhone1())
            .shippingAddress(shipStr)
            .discountCode(o.getDiscountCode())
            .discountValue(o.getDiscountValue())
            .build();
    }

    private static String formatAddressOneLine(Address a) {
        return Stream.of(a.getFirstLine(), a.getMunicipality(), a.getCity())
            .filter(Objects::nonNull)
            .filter(s -> !s.isBlank())
            .collect(Collectors.joining(", "));
    }

    private CustomerAdminPojo.CustomerShippingAddressPojo toShippingAddress(AddressBook ab, Person person) {
        Address a = ab.getAddress();
        String displayName = String.format("%s %s",
            person.getFirstName() != null ? person.getFirstName() : "",
            person.getLastName() != null ? person.getLastName() : "").trim();
        return CustomerAdminPojo.CustomerShippingAddressPojo.builder()
            .id(ab.getId())
            .label(ab.getLabel())
            .name(displayName.isEmpty() ? null : displayName)
            .phone(person.getPhone1())
            .email(person.getEmail())
            .address1(a.getFirstLine())
            .address2(a.getSecondLine())
            .ward(a.getWardCode())
            .district(a.getMunicipality())
            .city(a.getCity())
            .isDefault(ab.isDefaultShipping())
            .build();
    }
}
