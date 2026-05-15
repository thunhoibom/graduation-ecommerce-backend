package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.config.cache.CacheNames;
import org.monostudio.jpa.entities.PromotionRule;
import org.monostudio.jpa.entities.PromotionRuleAction;
import org.monostudio.jpa.entities.PromotionRuleCondition;
import org.monostudio.jpa.entities.PromotionScope;
import org.monostudio.jpa.repositories.PromotionRulesRepository;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/promotion-rules")
@Tag(name = "Promotion rules (admin)")
public class DataPromotionRulesController {

    private final PromotionRulesRepository promotionRulesRepository;

    @Autowired
    public DataPromotionRulesController(PromotionRulesRepository promotionRulesRepository) {
        this.promotionRulesRepository = promotionRulesRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List all promotion rules")
    @PreAuthorize("hasAuthority('discountCodes:read')")
    public List<PromotionRule> list() {
        return promotionRulesRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(CREATED)
    @Operation(summary = "Create a promotion rule with conditions and actions")
    @PreAuthorize("hasAuthority('discountCodes:create')")
    @CacheEvict(cacheNames = { CacheNames.ACTIVE_PROMOTION_RULES, CacheNames.PUBLIC_DISCOUNT_VALIDATION }, allEntries = true)
    public PromotionRule create(@RequestBody PromotionRule body) {
        if (body.getScope() == null) {
            body.setScope(PromotionScope.CART);
        }
        wireChildren(body);
        body.setId(null);
        return promotionRulesRepository.saveAndFlush(body);
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Replace an existing promotion rule (conditions and actions)")
    @PreAuthorize("hasAuthority('discountCodes:update')")
    @CacheEvict(cacheNames = { CacheNames.ACTIVE_PROMOTION_RULES, CacheNames.PUBLIC_DISCOUNT_VALIDATION }, allEntries = true)
    public PromotionRule replace(@PathVariable Long id, @RequestBody PromotionRule body) {
        PromotionRule existing = promotionRulesRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Promotion rule not found"));
        existing.setName(body.getName());
        existing.setPriority(body.getPriority());
        existing.setCombinable(body.isCombinable());
        existing.setActive(body.isActive());
        if (body.getScope() != null) {
            existing.setScope(body.getScope());
        }
        existing.setActiveFrom(body.getActiveFrom());
        existing.setActiveUntil(body.getActiveUntil());
        existing.setMutualExclusionGroup(body.getMutualExclusionGroup());

        existing.getConditions().clear();
        existing.getActions().clear();
        if (body.getConditions() != null) {
            for (PromotionRuleCondition c : body.getConditions()) {
                c.setId(null);
                c.setRule(existing);
                existing.getConditions().add(c);
            }
        }
        if (body.getActions() != null) {
            for (PromotionRuleAction a : body.getActions()) {
                a.setId(null);
                a.setRule(existing);
                existing.getActions().add(a);
            }
        }
        return promotionRulesRepository.saveAndFlush(existing);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    @Operation(summary = "Delete a promotion rule")
    @PreAuthorize("hasAuthority('discountCodes:delete')")
    @CacheEvict(cacheNames = { CacheNames.ACTIVE_PROMOTION_RULES, CacheNames.PUBLIC_DISCOUNT_VALIDATION }, allEntries = true)
    public void delete(@PathVariable Long id) {
        promotionRulesRepository.deleteById(id);
    }

    private void wireChildren(PromotionRule body) {
        if (body.getConditions() != null) {
            for (PromotionRuleCondition c : body.getConditions()) {
                c.setId(null);
                c.setRule(body);
            }
        }
        if (body.getActions() != null) {
            for (PromotionRuleAction a : body.getActions()) {
                a.setId(null);
                a.setRule(body);
            }
        }
    }
}
