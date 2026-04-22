package org.monostudio.api.services;

import org.monostudio.jpa.entities.PromotionRule;

import java.util.List;

public interface ActivePromotionRulesService {

    List<PromotionRule> loadActiveRules();
}
