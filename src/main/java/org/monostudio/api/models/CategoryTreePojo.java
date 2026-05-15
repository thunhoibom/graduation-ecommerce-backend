package org.monostudio.api.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Category tree node — carries a hierarchical children list for
 * public browsing (GET /api/public/categories).
 * Extends ProductCategoryPojo so it already has id, code, name, parent, productCount.
 */
@Getter
@Setter
@SuperBuilder
public class CategoryTreePojo extends ProductCategoryPojo {

    public CategoryTreePojo() {
        this.children = new ArrayList<>();
    }

    private List<CategoryTreePojo> children = new ArrayList<>();
}
