package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.QUserRole;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class UserRolesSortSpec {
    private static final QUserRole BASE_PATH = QUserRole.userRole;
    public static final Map<String, OrderSpecifier<?>> orderSpecMap = Map.of(
        "name", BASE_PATH.name.asc()
    );
}
