package org.monostudio.api.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.services.CustomerIdentityService;
import org.monostudio.common.EmailNormalizer;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.jpa.services.conversion.CustomersConverterService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerIdentityServiceImpl
    implements CustomerIdentityService {

    private final CustomersRepository customersRepository;
    private final UsersRepository usersRepository;
    private final CustomersConverterService customersConverterService;

    public CustomerIdentityServiceImpl(
        CustomersRepository customersRepository,
        UsersRepository usersRepository,
        CustomersConverterService customersConverterService
    ) {
        this.customersRepository = customersRepository;
        this.usersRepository = usersRepository;
        this.customersConverterService = customersConverterService;
    }

    @Override
    public String normalizeEmail(String email) {
        return EmailNormalizer.normalize(email);
    }

    @Override
    public Optional<Person> findPersonByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return Optional.empty();
        }
        return customersRepository.findAllByPersonEmailIgnoreCase(normalizedEmail).stream()
            .map(Customer::getPerson)
            .findFirst();
    }

    @Override
    public Optional<Customer> findCustomerByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return Optional.empty();
        }
        return selectCanonicalCustomer(customersRepository.findAllByPersonEmailIgnoreCase(normalizedEmail));
    }

    @Override
    public Customer ensureCustomerForPerson(Person person) {
        return customersRepository.findByPersonId(person.getId())
            .orElseGet(() -> customersRepository.saveAndFlush(
                Customer.builder()
                    .person(person)
                    .build()
            ));
    }

    @Override
    public Customer resolveCustomerForOrder(PersonPojo customerInfo) throws BadInputException {
        if (customerInfo == null) {
            throw new BadInputException("Customer information is required.");
        }

        String normalizedEmail = normalizeEmail(customerInfo.getEmail());
        if (normalizedEmail != null) {
            customerInfo.setEmail(normalizedEmail);
        }

        if (StringUtils.isNotBlank(customerInfo.getIdNumber())) {
            Optional<Customer> byIdNumber = customersRepository.findByPersonIdNumber(customerInfo.getIdNumber());
            if (byIdNumber.isPresent()) {
                return byIdNumber.get();
            }
        }

        if (normalizedEmail != null) {
            Optional<Customer> byEmail = findCustomerByEmail(normalizedEmail);
            if (byEmail.isPresent()) {
                return byEmail.get();
            }

            Optional<User> existingUser = usersRepository.findByPersonEmailIgnoreCase(normalizedEmail);
            if (existingUser.isPresent() && existingUser.get().getPerson() != null) {
                return ensureCustomerForPerson(existingUser.get().getPerson());
            }
        }

        return customersConverterService.convertToNewEntity(customerInfo);
    }

    @Override
    public Optional<Customer> findCustomerForUserName(String userName) {
        if (StringUtils.isBlank(userName)) {
            return Optional.empty();
        }

        Optional<User> user = usersRepository.findByNameWithProfile(userName);
        if (user.isEmpty() || user.get().getPerson() == null) {
            return Optional.empty();
        }

        Person person = user.get().getPerson();
        Optional<Customer> linkedCustomer = customersRepository.findByPersonId(person.getId());
        if (linkedCustomer.isPresent()) {
            return linkedCustomer;
        }

        return findCustomerByEmail(person.getEmail());
    }

    private Optional<Customer> selectCanonicalCustomer(List<Customer> customers) {
        if (customers.isEmpty()) {
            return Optional.empty();
        }
        if (customers.size() == 1) {
            return Optional.of(customers.get(0));
        }

        return customers.stream()
            .sorted(Comparator
                .comparing((Customer customer) -> usersRepository.existsByPerson_Id(customer.getPerson().getId()) ? 0 : 1)
                .thenComparing(Customer::getId))
            .findFirst();
    }
}
