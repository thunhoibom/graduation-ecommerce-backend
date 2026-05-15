package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QPerson;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class PeopleSortSpec {
    private static final QPerson BASE_PATH = QPerson.person;
    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "idNumber", BASE_PATH.idNumber.asc(),
        "firstName", BASE_PATH.firstName.asc(),
        "email", BASE_PATH.email.asc(),
        "phone1", BASE_PATH.phone1.asc(),
        "phone2", BASE_PATH.phone2.asc(),
        "name", BASE_PATH.lastName.asc(),
        "lastName", BASE_PATH.lastName.asc()
    );
}
