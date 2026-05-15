package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.AddressBook;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface AddressBookRepository
    extends Repository<AddressBook> {

    List<AddressBook> findByUserId(Long userId);

    Optional<AddressBook> findByUserIdAndLabel(Long userId, String label);

    @Query("SELECT ab FROM AddressBook ab JOIN FETCH ab.address WHERE ab.user.id = :userId")
    List<AddressBook> findByUserIdWithAddress(@Param("userId") Long userId);

    @Query("SELECT ab FROM AddressBook ab JOIN FETCH ab.address WHERE ab.user.id = :userId AND ab.defaultShipping = true")
    Optional<AddressBook> findDefaultShippingByUserId(@Param("userId") Long userId);

    @Query("SELECT ab FROM AddressBook ab JOIN FETCH ab.address WHERE ab.user.id = :userId AND ab.defaultBilling = true")
    Optional<AddressBook> findDefaultBillingByUserId(@Param("userId") Long userId);

    Optional<AddressBook> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
