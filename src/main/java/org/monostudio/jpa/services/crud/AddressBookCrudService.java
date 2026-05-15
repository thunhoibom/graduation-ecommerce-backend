package org.monostudio.jpa.services.crud;

import org.monostudio.api.models.AddressBookPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.AddressBook;
import org.monostudio.jpa.services.CrudService;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AddressBookCrudService
    extends CrudService<AddressBookPojo, AddressBook> {

    /**
     * Create a new address book entry for the given user.
     */
    AddressBookPojo createForUser(AddressBookPojo input, Long userId) throws BadInputException;

    /**
     * List all address book entries for a user.
     */
    List<AddressBookPojo> readManyByUser(Long userId);

    /**
     * Read a single entry by id (ensuring it belongs to the user).
     */
    Optional<AddressBookPojo> readOneByIdAndUser(Long id, Long userId);

    /**
     * Partially update an entry by id.
     */
    AddressBookPojo partialUpdate(Map<String, Object> changes, Long id, Long userId) throws BadInputException;

    /**
     * Replace an entry by id.
     */
    AddressBookPojo replace(AddressBookPojo input, Long id, Long userId) throws BadInputException;

    /**
     * Delete an entry by id.
     */
    void delete(Long id, Long userId) throws EntityNotFoundException;
}
