package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QAddressBook;
import org.monostudio.jpa.services.PredicateService;

public interface AddressBookPredicateService
    extends PredicateService {
    QAddressBook basePath = QAddressBook.addressBook;
}
