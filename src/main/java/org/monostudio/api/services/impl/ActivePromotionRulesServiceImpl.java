package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.services.ActivePromotionRulesService;
import org.monostudio.jpa.entities.PromotionRuleAction;
import org.monostudio.jpa.entities.PromotionRuleCondition;
import org.monostudio.config.cache.CacheNames;
import org.monostudio.jpa.entities.PromotionRule;
import org.monostudio.jpa.repositories.PromotionRulesRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ActivePromotionRulesServiceImpl implements ActivePromotionRulesService {

    private final PromotionRulesRepository promotionRulesRepository;

    @Autowired
    public ActivePromotionRulesServiceImpl(PromotionRulesRepository promotionRulesRepository) {
        this.promotionRulesRepository = promotionRulesRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.ACTIVE_PROMOTION_RULES, key = "'all'")
    public List<PromotionRule> loadActiveRules() {
        return promotionRulesRepository.findAllActive(LocalDateTime.now()).stream()
            .map(this::toSnapshot)
            .collect(Collectors.toList());
    }

    /**
     * Build a detached, cache-safe snapshot to avoid storing Hibernate PersistentCollection in Redis.
     */
    private PromotionRule toSnapshot(PromotionRule source) {
        PromotionRule target = new PromotionRule();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setPriority(source.getPriority());
        target.setCombinable(source.isCombinable());
        target.setActive(source.isActive());
        target.setScope(source.getScope());
        target.setActiveFrom(source.getActiveFrom());
        target.setActiveUntil(source.getActiveUntil());
        target.setMutualExclusionGroup(source.getMutualExclusionGroup());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setConditions(copyConditions(source.getConditions()));
        target.setActions(copyActions(source.getActions()));
        return target;
    }

    private Set<PromotionRuleCondition> copyConditions(Set<PromotionRuleCondition> conditions) {
        Set<PromotionRuleCondition> out = new LinkedHashSet<>();
        if (conditions == null) {
            return out;
        }
        for (PromotionRuleCondition c : conditions) {
            PromotionRuleCondition cc = new PromotionRuleCondition();
            cc.setId(c.getId());
            cc.setFactField(c.getFactField());
            cc.setOperator(c.getOperator());
            cc.setTargetValue(c.getTargetValue());
            out.add(cc);
        }
        return out;
    }

    private Set<PromotionRuleAction> copyActions(Set<PromotionRuleAction> actions) {
        Set<PromotionRuleAction> out = new LinkedHashSet<>();
        if (actions == null) {
            return out;
        }
        for (PromotionRuleAction a : actions) {
            PromotionRuleAction aa = new PromotionRuleAction();
            aa.setId(a.getId());
            aa.setActionType(a.getActionType());
            aa.setValue(a.getValue());
            out.add(aa);
        }
        return out;
    }
}
