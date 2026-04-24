package org.monostudio.api.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.monostudio.api.services.ActivePromotionRulesService;
import org.monostudio.api.services.ProductPricingSnapshotService;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.PromotionScope;
import org.monostudio.jpa.entities.PromotionRule;
import org.monostudio.jpa.entities.PromotionRuleAction;
import org.monostudio.jpa.entities.PromotionRuleCondition;
import org.monostudio.pricing.engine.PromotionConflictResolver;
import org.monostudio.pricing.engine.PromotionRuleEngine;
import org.monostudio.pricing.model.PricingFacts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ProductPricingSnapshotServiceImpl implements ProductPricingSnapshotService {

    private static final String FACT_ANY_PRODUCT = "cart.has_any_product";
    private static final String FACT_ANY_CATEGORY = "cart.has_any_category";

    private final ActivePromotionRulesService activePromotionRulesService;
    private final PromotionRuleEngine promotionRuleEngine;
    private final int maxPromotionStack;

    @Autowired
    public ProductPricingSnapshotServiceImpl(
        ActivePromotionRulesService activePromotionRulesService,
        PromotionRuleEngine promotionRuleEngine,
        @Value("${monostudio.pricing.max-promotion-stack:2}") int maxPromotionStack
    ) {
        this.activePromotionRulesService = activePromotionRulesService;
        this.promotionRuleEngine = promotionRuleEngine;
        this.maxPromotionStack = maxPromotionStack;
    }

    @Override
    public ProductPricingSnapshot calculate(Product product) {
        int originalPrice = product.getPrice();
        if (originalPrice <= 0) {
            return new ProductPricingSnapshot(0, 0, 0, false, null, null);
        }

        PricingFacts facts = buildPricingFacts(product, originalPrice);
        List<PromotionRule> activeRules = activePromotionRulesService.loadActiveRules();
        List<PromotionRule> matched = promotionRuleEngine.findMatchingRules(activeRules, facts).stream()
            .filter(this::isCatalogScopedRule)
            .filter(this::hasPriceDiscountAction)
            .toList();

        if (matched.isEmpty()) {
            return new ProductPricingSnapshot(originalPrice, originalPrice, 0, false, null, null);
        }

        List<PromotionRule> selected = new PromotionConflictResolver(maxPromotionStack).resolve(matched);
        PromotionRule appliedRule = selected.stream()
            .max(Comparator.comparingInt(PromotionRule::getPriority))
            .orElse(null);

        if (appliedRule == null) {
            return new ProductPricingSnapshot(originalPrice, originalPrice, 0, false, null, null);
        }

        int discountAmount = promotionRuleEngine.computeEffect(appliedRule, originalPrice).cartDiscountAmount();
        int currentPrice = Math.max(0, originalPrice - discountAmount);
        boolean hasDiscount = currentPrice < originalPrice;
        int discountPercent = hasDiscount
            ? (int) Math.round(((originalPrice - currentPrice) * 100.0) / originalPrice)
            : 0;

        return new ProductPricingSnapshot(
            originalPrice,
            currentPrice,
            Math.max(0, Math.min(100, discountPercent)),
            hasDiscount,
            appliedRule.getActiveFrom(),
            appliedRule.getActiveUntil()
        );
    }

    private PricingFacts buildPricingFacts(Product product, int originalPrice) {
        Set<Long> categoryIds = product.getProductCategory() != null && product.getProductCategory().getId() != null
            ? Set.of(product.getProductCategory().getId())
            : Set.of();
        Set<String> productBarcodes = StringUtils.isNotBlank(product.getBarcode())
            ? Set.of(product.getBarcode().trim().toLowerCase())
            : Set.of();
        LocalDateTime now = LocalDateTime.now();

        return PricingFacts.builder()
            .cartSubtotal(originalPrice)
            .cartLineCount(1)
            .cartTotalUnits(1)
            .cartCategoryIds(categoryIds)
            .cartProductBarcodes(productBarcodes)
            .userTier("")
            .userMonthlySpendCents(0)
            .envIsHoliday(false)
            .envHourOfDay(now.getHour())
            .now(now)
            .build();
    }

    private boolean isCatalogScopedRule(PromotionRule rule) {
        if (rule.getScope() == PromotionScope.PRODUCT || rule.getScope() == PromotionScope.CATEGORY) {
            return true;
        }
        // Backward-compatible fallback for legacy rules without scope.
        return rule.getScope() == null
            && rule.getConditions() != null
            && rule.getConditions().stream().anyMatch(this::isProductScopedCondition);
    }

    private boolean isProductScopedCondition(PromotionRuleCondition condition) {
        String factField = condition.getFactField() == null ? "" : condition.getFactField().trim().toLowerCase();
        return FACT_ANY_PRODUCT.equals(factField) || FACT_ANY_CATEGORY.equals(factField);
    }

    private boolean hasPriceDiscountAction(PromotionRule rule) {
        if (rule.getActions() == null || rule.getActions().isEmpty()) {
            return false;
        }
        return rule.getActions().stream().anyMatch(action -> PromotionRuleAction.TYPE_PERCENTAGE_DISCOUNT.equals(action.getActionType())
            || PromotionRuleAction.TYPE_FIXED_DISCOUNT.equals(action.getActionType()));
    }
}
