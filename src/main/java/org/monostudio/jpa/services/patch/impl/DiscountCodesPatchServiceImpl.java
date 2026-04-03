package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.DiscountCodePojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.DiscountCode;
import org.monostudio.jpa.services.patch.DiscountCodesPatchService;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@NoArgsConstructor
public class DiscountCodesPatchServiceImpl
    implements DiscountCodesPatchService {

    @Override
    public DiscountCode patchExistingEntity(Map<String, Object> changes, DiscountCode existing)
        throws BadInputException {
        DiscountCode target = new DiscountCode(existing);

        patchString(changes, "code",       v -> { target.setCode(((String) v).toUpperCase().trim()); });
        patchString(changes, "description", target::setDescription);
        patchString(changes, "type",       target::setType);

        if (changes.containsKey("value")) {
            Integer value = (Integer) changes.get("value");
            if (value != null && value >= 0) target.setValue(value);
        }
        if (changes.containsKey("maxUses")) {
            target.setMaxUses(castInteger(changes.get("maxUses")));
        }
        if (changes.containsKey("maxUsesPerCustomer")) {
            target.setMaxUsesPerCustomer(castInteger(changes.get("maxUsesPerCustomer")));
        }
        if (changes.containsKey("minCartValue")) {
            target.setMinCartValue(castInteger(changes.get("minCartValue")));
        }
        if (changes.containsKey("validFrom")) {
            target.setValidFrom(castLocalDateTime(changes.get("validFrom")));
        }
        if (changes.containsKey("validUntil")) {
            target.setValidUntil(castLocalDateTime(changes.get("validUntil")));
        }
        if (changes.containsKey("active")) {
            Boolean active = (Boolean) changes.get("active");
            if (active != null) target.setActive(active);
        }

        return target;
    }

    @Override
    public DiscountCode patchExistingEntity(DiscountCodePojo changes, DiscountCode existing)
        throws BadInputException {
        throw new UnsupportedOperationException("This method signature has been deprecated");
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private void patchString(Map<String, Object> changes, String key, java.util.function.Consumer<String> setter) {
        if (changes.containsKey(key)) {
            String val = (String) changes.get(key);
            if (val != null && !val.isBlank()) setter.accept(val);
        }
    }

    private Integer castInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        return null;
    }

    private LocalDateTime castLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDateTime) return (LocalDateTime) val;
        if (val instanceof String) return LocalDateTime.parse((String) val);
        return null;
    }
}
