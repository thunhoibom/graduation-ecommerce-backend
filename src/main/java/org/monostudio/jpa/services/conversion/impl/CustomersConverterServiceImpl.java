package org.monostudio.jpa.services.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.jpa.services.conversion.CustomersConverterService;
import org.monostudio.jpa.services.conversion.PeopleConverterService;

@Service
public class CustomersConverterServiceImpl
    implements CustomersConverterService {
    private final PeopleConverterService peopleConverterService;
    private final OrdersRepository ordersRepository;
    private final UsersRepository usersRepository;

    @Autowired
    public CustomersConverterServiceImpl(
        PeopleConverterService peopleConverterService,
        OrdersRepository ordersRepository,
        UsersRepository usersRepository
    ) {
        this.peopleConverterService = peopleConverterService;
        this.ordersRepository = ordersRepository;
        this.usersRepository = usersRepository;
    }

    @Override
    public PersonPojo convertToPojo(Customer source) {
        PersonPojo pojo = peopleConverterService.convertToPojo(source.getPerson());
        pojo.setCustomerId(source.getId());
        pojo.setOrderCount(ordersRepository.countForCustomer(source.getId()));
        pojo.setLoyaltyTier(source.getLoyaltyTier());
        pojo.setLoyaltyPointsBalance(source.getLoyaltyPointsBalance());
        pojo.setMonthlySpendCents(source.getMonthlySpendCents());
        pojo.setLinkedAccount(usersRepository.findByPersonId(source.getPerson().getId()).isPresent());
        return pojo;
    }

    @Override
    public Customer convertToNewEntity(PersonPojo source) throws BadInputException {
        Person targetPerson = peopleConverterService.convertToNewEntity(source);
        return Customer.builder()
            .person(targetPerson)
            .build();
    }

    @Override
    public Customer applyChangesToExistingEntity(PersonPojo source, Customer target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
