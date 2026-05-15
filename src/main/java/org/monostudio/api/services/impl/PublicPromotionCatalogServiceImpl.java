package org.monostudio.api.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PublicPromotionSummaryPojo;
import org.monostudio.api.services.ActivePromotionRulesService;
import org.monostudio.api.services.PublicPromotionCatalogService;
import org.monostudio.jpa.entities.PromotionRuleAction;
import org.monostudio.jpa.entities.PromotionRuleCondition;
import org.monostudio.jpa.entities.PromotionScope;
import org.monostudio.jpa.entities.PromotionRule;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.monostudio.jpa.entities.PromotionRuleAction.TYPE_FIXED_DISCOUNT;
import static org.monostudio.jpa.entities.PromotionRuleAction.TYPE_FREE_SHIPPING;
import static org.monostudio.jpa.entities.PromotionRuleAction.TYPE_PERCENTAGE_DISCOUNT;

@Service
public class PublicPromotionCatalogServiceImpl implements PublicPromotionCatalogService {

    private final ActivePromotionRulesService activePromotionRulesService;

    @Autowired
    public PublicPromotionCatalogServiceImpl(ActivePromotionRulesService activePromotionRulesService) {
        this.activePromotionRulesService = activePromotionRulesService;
    }

    @Override
    public List<PublicPromotionSummaryPojo> listPublicSummaries(String productBarcode) {
        List<PromotionRule> rules = activePromotionRulesService.loadActiveRules();
        String barcodeFilter = StringUtils.trimToEmpty(productBarcode).toLowerCase(Locale.ROOT);

        List<PublicPromotionSummaryPojo> out = new ArrayList<>();
        for (PromotionRule rule : rules) {
            if (!passesProductBarcodeFilter(rule, barcodeFilter)) {
                continue;
            }
            PromotionScope scope = rule.getScope() != null ? rule.getScope() : PromotionScope.CART;
            boolean catalogScope = scope == PromotionScope.PRODUCT || scope == PromotionScope.CATEGORY;

            String effects = summarizeEffects(rule);
            String conditions = summarizeConditions(rule);

            out.add(PublicPromotionSummaryPojo.builder()
                .id(rule.getId())
                .name(rule.getName())
                .scope(scope)
                .effectsSummary(effects)
                .conditionsSummary(conditions)
                .reflectedInProductPrice(catalogScope ? Boolean.TRUE : null)
                .activeUntil(rule.getActiveUntil())
                .build());
        }

        out.sort(Comparator.comparing(PublicPromotionSummaryPojo::getScope).thenComparing(p -> p.getName() == null ? "" : p.getName()));
        return out;
    }

    private boolean passesProductBarcodeFilter(PromotionRule rule, String barcodeLower) {
        if (barcodeLower.isEmpty()) {
            return true;
        }
        Set<PromotionRuleCondition> conds = rule.getConditions();
        if (conds == null || conds.isEmpty()) {
            return true;
        }
        boolean hasProductRestriction = false;
        boolean barcodeMatches = false;
        for (PromotionRuleCondition c : conds) {
            if (c.getFactField() == null) {
                continue;
            }
            if ("cart.has_any_product".equalsIgnoreCase(c.getFactField().trim())) {
                hasProductRestriction = true;
                if ("IN".equalsIgnoreCase(StringUtils.trimToEmpty(c.getOperator()))) {
                    Set<String> targets = Arrays.stream(StringUtils.trimToEmpty(c.getTargetValue()).split(","))
                        .map(s -> s.trim().toLowerCase(Locale.ROOT))
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());
                    if (targets.contains(barcodeLower)) {
                        barcodeMatches = true;
                    }
                }
            }
        }
        if (!hasProductRestriction) {
            return true;
        }
        return barcodeMatches;
    }

    private static String summarizeEffects(PromotionRule rule) {
        Set<PromotionRuleAction> actions = rule.getActions();
        if (actions == null || actions.isEmpty()) {
            return "Ưu đãi — xem điều kiện áp dụng";
        }
        List<String> parts = new ArrayList<>();
        for (PromotionRuleAction a : actions) {
            String t = StringUtils.trimToEmpty(a.getActionType());
            switch (t) {
                case TYPE_PERCENTAGE_DISCOUNT -> parts.add("Giảm " + Math.min(100, Math.max(0, a.getValue())) + "%");
                case TYPE_FIXED_DISCOUNT -> parts.add("Giảm " + formatVnd(a.getValue()));
                case TYPE_FREE_SHIPPING -> parts.add("Miễn phí vận chuyển");
                default -> parts.add("Ưu đãi đặc biệt");
            }
        }
        return String.join(" · ", parts);
    }

    private static String summarizeConditions(PromotionRule rule) {
        Set<PromotionRuleCondition> conds = rule.getConditions();
        if (conds == null || conds.isEmpty()) {
            return "Áp dụng theo quy tắc hệ thống";
        }
        List<String> parts = new ArrayList<>();
        List<PromotionRuleCondition> ordered = new ArrayList<>(conds);
        ordered.sort(Comparator.comparing(c -> c.getFactField() == null ? "" : c.getFactField()));
        for (PromotionRuleCondition c : ordered) {
            parts.add(describeCondition(c));
        }
        return String.join(" · ", parts);
    }

    private static String describeCondition(PromotionRuleCondition c) {
        String field = c.getFactField() == null ? "" : c.getFactField().trim();
        String op = c.getOperator() == null ? "" : c.getOperator().trim().toUpperCase(Locale.ROOT);
        String target = StringUtils.trimToEmpty(c.getTargetValue());

        if ("cart.subtotal".equalsIgnoreCase(field)) {
            int v = parseIntSafe(target);
            return switch (op) {
                case "GTE", ">=" -> "Đơn từ " + formatVnd(v);
                case "GT", ">" -> "Đơn trên " + formatVnd(v);
                case "LTE", "<=" -> "Đơn tối đa " + formatVnd(v);
                case "LT", "<" -> "Đơn dưới " + formatVnd(v);
                case "EQ", "==" -> "Đơn đạt đúng " + formatVnd(v);
                default -> "Điều kiện giá trị đơn";
            };
        }
        if ("cart.line_count".equalsIgnoreCase(field)) {
            return "Số dòng hàng trong giỏ " + op + " " + target;
        }
        if ("cart.total_units".equalsIgnoreCase(field)) {
            return "Tổng sản phẩm trong giỏ " + op + " " + target;
        }
        if ("cart.has_any_category".equalsIgnoreCase(field)) {
            return "Áp dụng khi có sản phẩm thuộc danh mục chỉ định";
        }
        if ("cart.has_any_product".equalsIgnoreCase(field)) {
            return "Áp dụng cho một số sản phẩm cụ thể";
        }
        if ("user.tier".equalsIgnoreCase(field)) {
            return "Theo hạng khách hàng";
        }
        if ("user.monthly_spend".equalsIgnoreCase(field)) {
            return "Theo chi tiêu trong tháng";
        }
        return "Điều kiện khuyến mãi";
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(StringUtils.trimToEmpty(s));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String formatVnd(int amount) {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
        return nf.format(amount) + " ₫";
    }
}
