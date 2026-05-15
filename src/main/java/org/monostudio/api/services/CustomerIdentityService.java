package org.monostudio.api.services;

import org.monostudio.api.models.PersonPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.Person;

import java.util.Optional;

public interface CustomerIdentityService {

    String normalizeEmail(String email);

    Optional<Person> findPersonByEmail(String email);

    Optional<Customer> findCustomerByEmail(String email);

    Customer ensureCustomerForPerson(Person person);

    Customer resolveCustomerForOrder(PersonPojo customerInfo) throws BadInputException;

    Optional<Customer> findCustomerForUserName(String userName);
}
