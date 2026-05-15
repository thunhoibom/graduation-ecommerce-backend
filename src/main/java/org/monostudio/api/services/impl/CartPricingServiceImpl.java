package org.monostudio.api.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.AppliedPromotionLine;
import org.monostudio.api.models.CartPricingResult;
import org.monostudio.api.models.DiscountValidationResult;
import org.monostudio.api.models.PromotionNearMissPojo;
import org.monostudio.api.services.ActivePromotionRulesService;
import org.monostudio.api.services.CartPricingService;
import org.monostudio.api.services.DiscountService;
import org.monostudio.api.services.ProductPricingSnapshotService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.CartItem;
import org.monostudio.jpa.entities.CartSession;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.entities.PromotionScope;
import org.monostudio.jpa.entities.PromotionRule;
import org.monostudio.jpa.entities.PromotionRuleCondition;
import org.monostudio.jpa.repositories.CartSessionsRepository;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.api.services.CustomerIdentityService;
import org.monostudio.pricing.engine.PromotionConflictResolver;
import org.monostudio.pricing.engine.PromotionRuleEngine;
import org.monostudio.pricing.engine.PromotionRuleEngine.RuleEffect;
import org.monostudio.pricing.model.PricingFacts;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.monostudio.jpa.entities.DiscountCode.TYPE_FREE_SHIPPING;

@Service
public class CartPricingServiceImpl implements CartPricingService {

    private final CartSessionsRepository cartSessionsRepository;
    private final CustomerIdentityService customerIdentityService;
    private final CustomersRepository customersRepository;
    private final ActivePromotionRulesService activePromotionRulesService;
    private final PromotionRuleEngine promotionRuleEngine;
    private final DiscountService discountService;
    private final ProductPricingSnapshotService productPricingSnapshotService;
    private final ObjectMapper objectMapper;

    private final int maxPromotionStack;

    @Autowired
    public CartPricingServiceImpl(
        CartSessionsRepository cartSessionsRepository,
        CustomerIdentityService customerIdentityService,
        CustomersRepository customersRepository,
        ActivePromotionRulesService activePromotionRulesService,
        PromotionRuleEngine promotionRuleEngine,
        DiscountService discountService,
        ProductPricingSnapshotService productPricingSnapshotService,
        ObjectMapper objectMapper,
        @Value("${monostudio.pricing.max-promotion-stack:2}") int maxPromotionStack
    ) {
        this.cartSessionsRepository = cartSessionsRepository;
        this.customerIdentityService = customerIdentityService;
        this.customersRepository = customersRepository;
        this.activePromotionRulesService = activePromotionRulesService;
        this.promotionRuleEngine = promotionRuleEngine;
        this.discountService = discountService;
        this.productPricingSnapshotService = productPricingSnapshotService;
        this.objectMapper = objectMapper;
        this.maxPromotionStack = maxPromotionStack;
    }

    @Override
    @Transactional
    public CartPricingResult calculate(String sessionToken, String couponCode, boolean persist) throws BadInputException {
        if (StringUtils.isBlank(sessionToken)) {
            throw new BadInputException("X-Session-Token header is required");
        }
        CartSession cart = cartSessionsRepository.findByTokenDeep(sessionToken)
            .orElseThrow(() -> new BadInputException("Cart session not found"));
        CartPricingResult result = calculateForSession(cart, couponCode, resolveCustomerIdForPricing(cart));
        if (persist) {
            persistSnapshot(cart, result);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CartPricingResult calculateForSession(CartSession cart, String couponCode, Long customerId)
        throws BadInputException {
        List<CartItem> items = cart.getItems() == null ? List.of() : cart.getItems().stream().toList();
        int subtotal = computeSubtotal(items);
        PricingFacts facts = buildFacts(cart, items, subtotal, customerId);

        List<PromotionRule> active = activePromotionRulesService.loadActiveRules();
        List<PromotionRule> matchedCheckout = promotionRuleEngine.findMatchingRules(active, facts).stream()
            .filter(this::isCheckoutScopedRule)
            .toList();
        List<PromotionNearMissPojo> promotionNearMisses = computePromotionNearMisses(active, matchedCheckout, facts);
        PromotionConflictResolver resolver = new PromotionConflictResolver(maxPromotionStack);
        List<PromotionRule> selected = resolver.resolve(matchedCheckout);

        int remaining = subtotal;
        int ruleDiscountSum = 0;
        boolean freeShipping = false;
        List<AppliedPromotionLine> lines = new ArrayList<>();

        for (PromotionRule rule : selected) {
            RuleEffect effect = promotionRuleEngine.computeEffect(rule, remaining);
            int d = effect.cartDiscountAmount();
            ruleDiscountSum += d;
            remaining = Math.max(0, remaining - d);
            freeShipping = freeShipping || effect.freeShipping();
            if (d > 0 || effect.freeShipping()) {
                lines.add(AppliedPromotionLine.builder()
                    .promotionRuleId(rule.getId())
                    .name(rule.getName())
                    .discountAmount(d > 0 ? d : null)
                    .freeShipping(effect.freeShipping() ? Boolean.TRUE : null)
                    .build());
            }
        }

        int couponOff = 0;
        String appliedCode = null;
        if (StringUtils.isNotBlank(couponCode)) {
            DiscountValidationResult dr = discountService.validateDiscount(couponCode.trim(), subtotal, customerId);
            if (!dr.isValid()) {
                throw new BadInputException(dr.getMessage() != null ? dr.getMessage() : "Invalid discount code");
            }
            couponOff = dr.getDiscountAmount();
            appliedCode = dr.getCode();
            freeShipping = freeShipping || TYPE_FREE_SHIPPING.equals(dr.getType());
            lines.add(AppliedPromotionLine.builder()
                .name(dr.getMessage() != null ? dr.getMessage() : "Coupon " + dr.getCode())
                .couponCode(dr.getCode())
                .discountAmount(TYPE_FREE_SHIPPING.equals(dr.getType()) ? 0 : couponOff)
                .freeShipping(TYPE_FREE_SHIPPING.equals(dr.getType()) ? Boolean.TRUE : null)
                .build());
        }

        int totalDiscount = Math.min(subtotal, ruleDiscountSum + couponOff);
        int totalAfter = Math.max(0, subtotal - totalDiscount);

        return CartPricingResult.builder()
            .subtotal(subtotal)
            .discountAmount(totalDiscount)
            .totalAfterDiscount(totalAfter)
            .freeShipping(freeShipping)
            .appliedPromotions(lines)
            .promotionNearMisses(promotionNearMisses.isEmpty() ? null : promotionNearMisses)
            .appliedDiscountCode(appliedCode)
            .build();
    }

    private void persistSnapshot(CartSession cart, CartPricingResult result) throws BadInputException {
        cart.setAppliedDiscountCode(result.getAppliedDiscountCode());
        cart.setDiscountAmount(result.getDiscountAmount());
        try {
            cart.setAppliedPromotionsJson(
                result.getAppliedPromotions() == null || result.getAppliedPromotions().isEmpty()
                    ? null
                    : objectMapper.writeValueAsString(result.getAppliedPromotions())
            );
        } catch (JsonProcessingException e) {
            throw new BadInputException("Could not serialize applied promotions");
        }
        cartSessionsRepository.saveAndFlush(cart);
    }

    private Long resolveCustomerIdForPricing(CartSession cart) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && StringUtils.isNotBlank(auth.getName())) {
            Optional<Customer> customer = customerIdentityService.findCustomerForUserName(auth.getName());
            if (customer.isPresent()) {
                return customer.get().getId();
            }
        }
        if (cart.getCustomer() != null && cart.getCustomer().getId() != null) {
            return cart.getCustomer().getId();
        }
        return null;
    }

    private int computeSubtotal(List<CartItem> items) {
        int sum = 0;
        for (CartItem item : items) {
            if (item.getVariant() == null || item.getVariant().getProduct() == null) {
                continue;
            }
            var productPricing = productPricingSnapshotService.calculate(item.getVariant().getProduct());
            int unit = productPricing.currentPrice() + item.getVariant().getPriceModifier();
            sum += unit * item.getQuantity();
        }
        return sum;
    }

    private PricingFacts buildFacts(CartSession cart, List<CartItem> items, int subtotal, Long customerId) {
        Set<Long> categoryIds = new HashSet<>();
        Set<String> productBarcodes = new HashSet<>();
        int lines = 0;
        int units = 0;
        for (CartItem item : items) {
            lines++;
            units += item.getQuantity();
            if (item.getVariant() != null && item.getVariant().getProduct() != null) {
                String barcode = item.getVariant().getProduct().getBarcode();
                if (StringUtils.isNotBlank(barcode)) {
                    productBarcodes.add(barcode.trim().toLowerCase());
                }
                ProductCategory cat = item.getVariant().getProduct().getProductCategory();
                if (cat != null) {
                    categoryIds.add(cat.getId());
                }
            }
        }

        String tier = "";
        int monthly = 0;
        Long cid = customerId;
        if (cid == null && cart.getCustomer() != null) {
            cid = cart.getCustomer().getId();
        }
        if (cid != null) {
            Optional<Customer> c = customersRepository.findById(cid);
            if (c.isPresent()) {
                tier = StringUtils.trimToEmpty(c.get().getLoyaltyTier());
                monthly = c.get().getMonthlySpendCents();
            }
        }

        LocalDateTime now = LocalDateTime.now();
        return PricingFacts.builder()
            .cartSubtotal(subtotal)
            .cartLineCount(lines)
            .cartTotalUnits(units)
            .cartCategoryIds(categoryIds)
            .cartProductBarcodes(productBarcodes)
            .userTier(tier)
            .userMonthlySpendCents(monthly)
            .envIsHoliday(false)
            .envHourOfDay(now.getHour())
            .now(now)
            .build();
    }

    private boolean isCheckoutScopedRule(PromotionRule rule) {
        if (rule.getScope() == null) {
            // Legacy rules created before scope support are treated as cart-scope.
            return true;
        }
        return rule.getScope() == PromotionScope.CART || rule.getScope() == PromotionScope.SHIPPING;
    }

    private List<PromotionNearMissPojo> computePromotionNearMisses(
        List<PromotionRule> active,
        List<PromotionRule> matchedCheckout,
        PricingFacts facts
    ) {
        Set<Long> matchedIds = matchedCheckout.stream().map(PromotionRule::getId).collect(Collectors.toSet());
        List<PromotionRule> checkoutActive = active.stream()
            .filter(this::isCheckoutScopedRule)
            .collect(Collectors.toList());

        List<PromotionNearMissPojo> out = new ArrayList<>();
        int subtotal = facts.getCartSubtotal();

        for (PromotionRule rule : checkoutActive) {
            if (matchedIds.contains(rule.getId())) {
                continue;
            }
            Set<PromotionRuleCondition> conds = rule.getConditions();
            if (conds == null || conds.isEmpty()) {
                continue;
            }

            Integer maxRequiredMin = null;
            boolean ineligible = false;

            for (PromotionRuleCondition c : conds) {
                if (promotionRuleEngine.singleConditionMatches(c, facts)) {
                    continue;
                }
                Integer floor = requiredMinSubtotalIfLowSubtotalFailure(c);
                if (floor == null) {
                    ineligible = true;
                    break;
                }
                maxRequiredMin = maxRequiredMin == null ? floor : Math.max(maxRequiredMin, floor);
            }

            if (ineligible || maxRequiredMin == null) {
                continue;
            }

            if (subtotal >= maxRequiredMin) {
                continue;
            }

            PricingFacts boosted = copyFactsWithSubtotal(facts, maxRequiredMin);
            if (!promotionRuleEngine.ruleConditionsMatch(rule, boosted)) {
                continue;
            }

            int remaining = maxRequiredMin - subtotal;
            if (remaining <= 0) {
                continue;
            }

            out.add(PromotionNearMissPojo.builder()
                .ruleId(rule.getId())
                .title(rule.getName())
                .remainingAmount(remaining)
                .messageVi(String.format(
                    "Thêm %s nữa để đủ điều kiện \"%s\".",
                    formatVndNearMiss(remaining),
                    StringUtils.defaultString(rule.getName())
                ))
                .build());
        }

        return out;
    }

    /** Chỉ khi điều kiện thất bại là cart.subtotal GTE/GT — trả về subtotal tối thiểu cần đạt. */
    private static Integer requiredMinSubtotalIfLowSubtotalFailure(PromotionRuleCondition c) {
        if (c.getFactField() == null || !"cart.subtotal".equalsIgnoreCase(c.getFactField().trim())) {
            return null;
        }
        String op = c.getOperator() == null ? "" : c.getOperator().trim().toUpperCase(Locale.ROOT);
        int target = parseIntSafeNearMiss(c.getTargetValue());
        return switch (op) {
            case "GTE", ">=" -> target;
            case "GT", ">" -> target + 1;
            default -> null;
        };
    }

    private static int parseIntSafeNearMiss(String s) {
        try {
            return Integer.parseInt(StringUtils.trimToEmpty(s));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static PricingFacts copyFactsWithSubtotal(PricingFacts facts, int cartSubtotal) {
        return PricingFacts.builder()
            .cartSubtotal(cartSubtotal)
            .cartLineCount(facts.getCartLineCount())
            .cartTotalUnits(facts.getCartTotalUnits())
            .cartCategoryIds(facts.getCartCategoryIds())
            .cartProductBarcodes(facts.getCartProductBarcodes())
            .userTier(facts.getUserTier())
            .userMonthlySpendCents(facts.getUserMonthlySpendCents())
            .envIsHoliday(facts.isEnvIsHoliday())
            .envHourOfDay(facts.getEnvHourOfDay())
            .now(facts.getNow())
            .build();
    }

    private static String formatVndNearMiss(int amount) {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
        return nf.format(amount) + " ₫";
    }
}
