package org.monostudio.pricing.engine;

import org.apache.commons.lang3.StringUtils;
import org.monostudio.jpa.entities.PromotionRuleCondition;
import org.monostudio.pricing.model.PricingFacts;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Evaluates DB-driven conditions against {@link PricingFacts} (no arbitrary SpEL).
 */
@Component
public class PromotionConditionEvaluator {

    public boolean ruleMatches(PromotionRuleCondition c, PricingFacts facts) {
        Comparable<?> left = resolveLeft(c.getFactField(), facts);
        if (left == null) {
            return false;
        }
        String op = c.getOperator() == null ? "EQ" : c.getOperator().trim().toUpperCase(Locale.ROOT);
        String rawTarget = c.getTargetValue();
        return switch (op) {
            case "EQ", "==" -> compareEqual(left, rawTarget, facts);
            case "NE", "!=" -> !compareEqual(left, rawTarget, facts);
            case "GT", ">" -> compareNumeric(left, rawTarget) > 0;
            case "GTE", ">=" -> compareNumeric(left, rawTarget) >= 0;
            case "LT", "<" -> compareNumeric(left, rawTarget) < 0;
            case "LTE", "<=" -> compareNumeric(left, rawTarget) <= 0;
            case "IN" -> memberOfSet(left, rawTarget);
            default -> false;
        };
    }

    public boolean allConditionsMatch(Iterable<PromotionRuleCondition> conditions, PricingFacts facts) {
        for (PromotionRuleCondition c : conditions) {
            if (!ruleMatches(c, facts)) {
                return false;
            }
        }
        return true;
    }

    private Comparable<?> resolveLeft(String field, PricingFacts facts) {
        if (field == null) {
            return null;
        }
        String f = field.trim();
        return switch (f) {
            case "cart.subtotal" -> facts.getCartSubtotal();
            case "cart.line_count" -> facts.getCartLineCount();
            case "cart.total_units" -> facts.getCartTotalUnits();
            case "user.tier" -> facts.getUserTier() == null ? "" : facts.getUserTier();
            case "user.monthly_spend" -> facts.getUserMonthlySpendCents();
            case "env.is_holiday" -> facts.isEnvIsHoliday() ? 1 : 0;
            case "env.hour_of_day" -> facts.getEnvHourOfDay();
            default -> null;
        };
    }

    private boolean compareEqual(Comparable<?> left, String rawTarget, PricingFacts facts) {
        if (left instanceof Number n) {
            int target = parseIntSafe(rawTarget);
            return n.intValue() == target;
        }
        if (left instanceof String s) {
            return s.equalsIgnoreCase(StringUtils.trimToEmpty(rawTarget));
        }
        return false;
    }

    private int compareNumeric(Comparable<?> left, String rawTarget) {
        if (!(left instanceof Number n)) {
            return 0;
        }
        int target = parseIntSafe(rawTarget);
        return Integer.compare(n.intValue(), target);
    }

    private boolean memberOfSet(Comparable<?> left, String rawTarget) {
        if (left == null || rawTarget == null) {
            return false;
        }
        Set<String> allowed = Arrays.stream(rawTarget.split(","))
            .map(s -> s.trim().toLowerCase(Locale.ROOT))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
        if (left instanceof Number n) {
            return allowed.contains(String.valueOf(n.longValue()));
        }
        return allowed.contains(left.toString().toLowerCase(Locale.ROOT));
    }

    /**
     * True if any cart line's product category id is in the IN list (target_value comma-separated ids).
     */
    public boolean categoryAnyMatch(String rawTarget, PricingFacts facts) {
        if (rawTarget == null || facts.getCartCategoryIds() == null) {
            return false;
        }
        Set<Long> targets = Arrays.stream(rawTarget.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(PromotionConditionEvaluator::parseLongSafe)
            .collect(Collectors.toSet());
        return facts.getCartCategoryIds().stream().anyMatch(targets::contains);
    }

    /**
     * True if any cart line's product barcode is in the IN list (target_value comma-separated barcodes).
     */
    public boolean productAnyMatch(String rawTarget, PricingFacts facts) {
        if (rawTarget == null || facts.getCartProductBarcodes() == null) {
            return false;
        }
        Set<String> targets = Arrays.stream(rawTarget.split(","))
            .map(String::trim)
            .map(s -> s.toLowerCase(Locale.ROOT))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
        return facts.getCartProductBarcodes().stream()
            .map(s -> s == null ? "" : s.toLowerCase(Locale.ROOT))
            .anyMatch(targets::contains);
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(StringUtils.trimToEmpty(s));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long parseLongSafe(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
