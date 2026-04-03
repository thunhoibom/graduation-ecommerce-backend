package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QAddressBook;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class AddressBookSortSpec {
    private static final QAddressBook BASE_PATH = QAddressBook.addressBook;

    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "id", BASE_PATH.id.asc(),
        "label", BASE_PATH.label.asc(),
        "defaultShipping", BASE_PATH.defaultShipping.asc(),
        "defaultBilling", BASE_PATH.defaultBilling.asc(),
        "createdAt", BASE_PATH.createdAt.desc()
    );
}
