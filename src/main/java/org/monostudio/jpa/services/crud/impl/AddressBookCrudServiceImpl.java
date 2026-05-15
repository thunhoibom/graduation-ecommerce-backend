package org.monostudio.jpa.services.crud.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.AddressBookPojo;
import org.monostudio.api.models.AddressPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Address;
import org.monostudio.jpa.entities.AddressBook;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.repositories.AddressBookRepository;
import org.monostudio.jpa.repositories.AddressesRepository;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.jpa.services.conversion.AddressBookConverterService;
import org.monostudio.jpa.services.conversion.AddressesConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.AddressBookCrudService;
import org.monostudio.jpa.services.patch.AddressBookPatchService;

import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AddressBookCrudServiceImpl
    extends CrudGenericService<AddressBookPojo, AddressBook>
    implements AddressBookCrudService {

    private final AddressBookRepository addressBookRepository;
    private final AddressBookConverterService addressBookConverterService;
    private final AddressesConverterService addressesConverterService;
    private final AddressesRepository addressesRepository;
    private final UsersRepository usersRepository;
    private final AddressBookPatchService addressBookPatchService;

    @Autowired
    public AddressBookCrudServiceImpl(
        AddressBookRepository addressBookRepository,
        AddressBookConverterService addressBookConverterService,
        AddressesConverterService addressesConverterService,
        AddressBookPatchService addressBookPatchService,
        AddressesRepository addressesRepository,
        UsersRepository usersRepository
    ) {
        super(addressBookRepository, addressBookConverterService, addressBookPatchService);
        this.addressBookRepository = addressBookRepository;
        this.addressBookConverterService = addressBookConverterService;
        this.addressesConverterService = addressesConverterService;
        this.addressesRepository = addressesRepository;
        this.usersRepository = usersRepository;
        this.addressBookPatchService = addressBookPatchService;
    }

    /**
     * Not supported — address book creation requires a userId.
     * Use {@link #createForUser(AddressBookPojo, Long)} instead.
     */
    @Override
    public AddressBookPojo create(AddressBookPojo input) {
        throw new UnsupportedOperationException(
            "AddressBook creation requires a userId. Use createForUser(input, userId) instead.");
    }

    /**
     * Create a new address book entry for the given user.
     */
    @Transactional
    public AddressBookPojo createForUser(AddressBookPojo input, Long userId) throws BadInputException {
        this.validateInputPojoBeforeCreation(input);

        User user = usersRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Build or reuse address
        Address address = buildAddressFromPojo(input.getAddress());
        Address savedAddress = addressesRepository.saveAndFlush(address);

        AddressBook addressBook = addressBookConverterService.convertToNewEntity(input);
        addressBook.setAddress(savedAddress);
        addressBook.setUser(user);

        // Clear other defaults if setting a new default
        if (Boolean.TRUE.equals(input.getDefaultShipping())) {
            clearDefaultShippingForUser(userId);
        }
        if (Boolean.TRUE.equals(input.getDefaultBilling())) {
            clearDefaultBillingForUser(userId);
        }

        AddressBook saved = addressBookRepository.saveAndFlush(addressBook);
        return addressBookConverterService.convertToPojo(saved);
    }

    /**
     * List all address book entries for the given user (with address fetched).
     */
    @Transactional(readOnly = true)
    public List<AddressBookPojo> readManyByUser(Long userId) {
        return addressBookRepository.findByUserIdWithAddress(userId).stream()
            .map(addressBookConverterService::convertToPojo)
            .collect(Collectors.toList());
    }

    /**
     * Read a single address book entry by id, ensuring it belongs to the user.
     */
    @Transactional(readOnly = true)
    public Optional<AddressBookPojo> readOneByIdAndUser(Long id, Long userId) {
        return addressBookRepository.findByIdAndUserId(id, userId)
            .map(addressBookConverterService::convertToPojo);
    }

    /**
     * Update an existing address book entry (partial update).
     */
    @Transactional
    public AddressBookPojo partialUpdate(Map<String, Object> changes, Long id, Long userId) throws BadInputException {
        AddressBook existing = addressBookRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new EntityNotFoundException("Address book entry not found"));

        // Handle address field updates
        if (changes.containsKey("address")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> addrChanges = (Map<String, Object>) changes.get("address");
            if (addrChanges != null) {
                Address address = existing.getAddress();
                applyAddressPatch(address, addrChanges);
                addressesRepository.saveAndFlush(address);
            }
        }

        // Handle default flags
        if (changes.containsKey("defaultShipping")
            && Boolean.TRUE.equals(changes.get("defaultShipping"))) {
            clearDefaultShippingForUser(userId);
        }
        if (changes.containsKey("defaultBilling")
            && Boolean.TRUE.equals(changes.get("defaultBilling"))) {
            clearDefaultBillingForUser(userId);
        }

        // Patch address book fields
        AddressBook patched = addressBookPatchService.patchExistingEntity(changes, existing);
        AddressBook saved = addressBookRepository.saveAndFlush(patched);
        return addressBookConverterService.convertToPojo(saved);
    }

    /**
     * Replace an address book entry entirely.
     */
    @Transactional
    public AddressBookPojo replace(AddressBookPojo input, Long id, Long userId) throws BadInputException {
        AddressBook existing = addressBookRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new EntityNotFoundException("Address book entry not found"));

        // Update address
        AddressPojo inputAddress = input.getAddress();
        if (inputAddress != null) {
            Address address = existing.getAddress();
            updateAddressFromPojo(address, inputAddress);
            addressesRepository.saveAndFlush(address);
        }

        // Handle default flags
        if (Boolean.TRUE.equals(input.getDefaultShipping())) {
            clearDefaultShippingForUser(userId);
        }
        if (Boolean.TRUE.equals(input.getDefaultBilling())) {
            clearDefaultBillingForUser(userId);
        }

        // Patch non-address fields
        Map<String, Object> changes = new HashMap<>();
        if (input.getLabel() != null) changes.put("label", input.getLabel());
        if (input.getDefaultShipping() != null) changes.put("defaultShipping", input.getDefaultShipping());
        if (input.getDefaultBilling() != null) changes.put("defaultBilling", input.getDefaultBilling());

        AddressBook patched = addressBookPatchService.patchExistingEntity(changes, existing);
        AddressBook saved = addressBookRepository.saveAndFlush(patched);
        return addressBookConverterService.convertToPojo(saved);
    }

    /**
     * Delete an address book entry.
     */
    @Transactional
    public void delete(Long id, Long userId) throws EntityNotFoundException {
        AddressBook addressBook = addressBookRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new EntityNotFoundException("Address book entry not found"));
        addressBookRepository.delete(addressBook);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Address buildAddressFromPojo(AddressPojo pojo) throws BadInputException {
        if (pojo == null) {
            throw new BadInputException("Address data is required");
        }
        return addressesConverterService.convertToNewEntity(pojo);
    }

    private void updateAddressFromPojo(Address address, AddressPojo pojo) {
        if (pojo.getFirstLine() != null) address.setFirstLine(pojo.getFirstLine());
        if (pojo.getSecondLine() != null) address.setSecondLine(pojo.getSecondLine());
        if (pojo.getMunicipality() != null) address.setMunicipality(pojo.getMunicipality());
        if (pojo.getCity() != null) address.setCity(pojo.getCity());
        if (pojo.getPostalCode() != null) address.setPostalCode(pojo.getPostalCode());
        if (pojo.getNotes() != null) address.setNotes(pojo.getNotes());
    }

    private void applyAddressPatch(Address address, Map<String, Object> changes) {
        if (changes.containsKey("firstLine")) address.setFirstLine((String) changes.get("firstLine"));
        if (changes.containsKey("secondLine")) address.setSecondLine((String) changes.get("secondLine"));
        if (changes.containsKey("municipality")) address.setMunicipality((String) changes.get("municipality"));
        if (changes.containsKey("city")) address.setCity((String) changes.get("city"));
        if (changes.containsKey("postalCode")) address.setPostalCode((String) changes.get("postalCode"));
        if (changes.containsKey("notes")) address.setNotes((String) changes.get("notes"));
    }

    private void clearDefaultShippingForUser(Long userId) {
        List<AddressBook> entries = addressBookRepository.findByUserId(userId);
        for (AddressBook entry : entries) {
            if (entry.isDefaultShipping()) {
                entry.setDefaultShipping(false);
                addressBookRepository.saveAndFlush(entry);
            }
        }
    }

    private void clearDefaultBillingForUser(Long userId) {
        List<AddressBook> entries = addressBookRepository.findByUserId(userId);
        for (AddressBook entry : entries) {
            if (entry.isDefaultBilling()) {
                entry.setDefaultBilling(false);
                addressBookRepository.saveAndFlush(entry);
            }
        }
    }

    @Override
    protected void validateInputPojoBeforeCreation(AddressBookPojo inputPojo) throws BadInputException {
        if (inputPojo.getLabel() == null || inputPojo.getLabel().isBlank()) {
            throw new BadInputException("Address label is required");
        }
    }

    /**
     * Not supported — duplicate check is handled in createForUser after resolving userId.
     */
    @Override
    public Optional<AddressBook> getExisting(AddressBookPojo input) throws BadInputException {
        return Optional.empty();
    }
}
