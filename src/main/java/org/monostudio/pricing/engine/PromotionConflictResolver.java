package org.monostudio.pricing.engine;

import org.apache.commons.lang3.StringUtils;
import org.monostudio.jpa.entities.PromotionRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Picks a subset of rules: highest priority first, max {@code maxStack} rules,
 * respects {@link PromotionRule#isCombinable()} and mutual exclusion groups.
 */
public final class PromotionConflictResolver {

    private final int maxStack;

    public PromotionConflictResolver(int maxStack) {
        this.maxStack = Math.max(1, maxStack);
    }

    public List<PromotionRule> resolve(List<PromotionRule> candidates) {
        List<PromotionRule> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingInt(PromotionRule::getPriority).reversed());

        List<PromotionRule> chosen = new ArrayList<>();
        Set<String> usedExclusionGroups = new HashSet<>();

        for (PromotionRule rule : sorted) {
            if (chosen.size() >= maxStack) {
                break;
            }
            if (!canAdd(chosen, rule, usedExclusionGroups)) {
                continue;
            }
            chosen.add(rule);
            if (StringUtils.isNotBlank(rule.getMutualExclusionGroup())) {
                usedExclusionGroups.add(rule.getMutualExclusionGroup());
            }
        }
        return chosen;
    }

    private boolean canAdd(List<PromotionRule> chosen, PromotionRule candidate, Set<String> usedExclusionGroups) {
        if (StringUtils.isNotBlank(candidate.getMutualExclusionGroup())
            && usedExclusionGroups.contains(candidate.getMutualExclusionGroup())) {
            return false;
        }
        if (chosen.isEmpty()) {
            return true;
        }
        if (!candidate.isCombinable()) {
            return false;
        }
        for (PromotionRule existing : chosen) {
            if (!existing.isCombinable()) {
                return false;
            }
            if (StringUtils.isNotBlank(candidate.getMutualExclusionGroup())
                && candidate.getMutualExclusionGroup().equals(existing.getMutualExclusionGroup())) {
                return false;
            }
        }
        return true;
    }
}
