package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QPerson;
import org.monostudio.jpa.entities.QSalesperson;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class SalespeopleSortSpec {
    private static final QSalesperson BASE_PATH = QSalesperson.salesperson;
    private static final QPerson PERSON_PATH = BASE_PATH.person;
    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "idNumber", PERSON_PATH.idNumber.asc(),
        "firstName", PERSON_PATH.firstName.asc(),
        "email", PERSON_PATH.email.asc(),
        "phone1", PERSON_PATH.phone1.asc(),
        "phone2", PERSON_PATH.phone2.asc(),
        "name", PERSON_PATH.lastName.asc(),
        "lastName", PERSON_PATH.lastName.asc()
    );
}
