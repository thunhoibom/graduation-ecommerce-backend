package org.monostudio.pricing.engine;

import org.monostudio.jpa.entities.PromotionRule;
import org.monostudio.jpa.entities.PromotionRuleAction;
import org.monostudio.jpa.entities.PromotionRuleCondition;
import org.monostudio.pricing.model.PricingFacts;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.monostudio.jpa.entities.PromotionRuleAction.TYPE_FIXED_DISCOUNT;
import static org.monostudio.jpa.entities.PromotionRuleAction.TYPE_FREE_SHIPPING;
import static org.monostudio.jpa.entities.PromotionRuleAction.TYPE_PERCENTAGE_DISCOUNT;

/**
 * Finds promotion rules whose conditions match facts.
 */
@Component
public class PromotionRuleEngine {

    private final PromotionConditionEvaluator conditionEvaluator;

    public PromotionRuleEngine(PromotionConditionEvaluator conditionEvaluator) {
        this.conditionEvaluator = conditionEvaluator;
    }

    public List<PromotionRule> findMatchingRules(List<PromotionRule> activeRules, PricingFacts facts) {
        List<PromotionRule> out = new ArrayList<>();
        for (PromotionRule rule : activeRules) {
            if (conditionsMatch(rule, facts)) {
                out.add(rule);
            }
        }
        return out;
    }

    private boolean conditionsMatch(PromotionRule rule, PricingFacts facts) {
        for (PromotionRuleCondition c : rule.getConditions()) {
            if (isCategoryPresenceCondition(c)) {
                if (!"IN".equalsIgnoreCase(c.getOperator() == null ? "" : c.getOperator().trim())) {
                    return false;
                }
                if (!conditionEvaluator.categoryAnyMatch(c.getTargetValue(), facts)) {
                    return false;
                }
            } else if (isProductPresenceCondition(c)) {
                if (!"IN".equalsIgnoreCase(c.getOperator() == null ? "" : c.getOperator().trim())) {
                    return false;
                }
                if (!conditionEvaluator.productAnyMatch(c.getTargetValue(), facts)) {
                    return false;
                }
            } else if (!conditionEvaluator.ruleMatches(c, facts)) {
                return false;
            }
        }
        return true;
    }

    private boolean isCategoryPresenceCondition(PromotionRuleCondition c) {
        return c.getFactField() != null
            && "cart.has_any_category".equalsIgnoreCase(c.getFactField().trim());
    }

    private boolean isProductPresenceCondition(PromotionRuleCondition c) {
        return c.getFactField() != null
            && "cart.has_any_product".equalsIgnoreCase(c.getFactField().trim());
    }

    /**
     * Computes cart-level discount (capped to subtotal) and whether free shipping applies.
     */
    public RuleEffect computeEffect(PromotionRule rule, int subtotal) {
        int totalPct = 0;
        int fixed = 0;
        boolean freeShip = false;
        for (PromotionRuleAction a : rule.getActions()) {
            String t = a.getActionType() == null ? "" : a.getActionType().trim();
            if (TYPE_PERCENTAGE_DISCOUNT.equals(t)) {
                totalPct += Math.max(0, a.getValue());
            } else if (TYPE_FIXED_DISCOUNT.equals(t)) {
                fixed += Math.max(0, a.getValue());
            } else if (TYPE_FREE_SHIPPING.equals(t)) {
                freeShip = true;
            }
        }
        totalPct = Math.min(100, totalPct);
        int fromPct = (int) Math.round(subtotal * (totalPct / 100.0));
        int discount = Math.min(subtotal, fromPct + fixed);
        return new RuleEffect(discount, freeShip);
    }

    public record RuleEffect(int cartDiscountAmount, boolean freeShipping) {
    }
}
