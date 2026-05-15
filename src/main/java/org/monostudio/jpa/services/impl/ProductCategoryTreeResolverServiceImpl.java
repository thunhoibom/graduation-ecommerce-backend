package org.monostudio.jpa.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.config.ApiProperties;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.repositories.ProductsCategoriesRepository;
import org.monostudio.jpa.services.ProductCategoryTreeResolverService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class ProductCategoryTreeResolverServiceImpl
    implements ProductCategoryTreeResolverService {
    private final ProductsCategoriesRepository repository;
    private final ApiProperties apiProperties;

    @Autowired
    public ProductCategoryTreeResolverServiceImpl(
        ProductsCategoriesRepository repository,
        ApiProperties apiProperties
    ) {
        this.repository = repository;
        this.apiProperties = apiProperties;
    }

    @Override
    public List<ProductCategory> getBranchesFromRoot(ProductCategory rootBranch) {
        int maxAllowedDepth = apiProperties.getMaxCategoryFetchingRecursionDepth();
        List<ProductCategory> immediateDescendants = repository.findByParent(rootBranch);
        List<ProductCategory> allBranches = new ArrayList<>();
        allBranches.add(rootBranch); // Include root!
        allBranches.addAll(immediateDescendants);
        for (ProductCategory branch : immediateDescendants) {
            this.recursivelyAddBranches(allBranches, branch, maxAllowedDepth);
        }
        return allBranches;
    }

    @Override
    public List<Long> getBranchIdsFromRootId(Long rootId) {
        int depth = apiProperties.getMaxCategoryFetchingRecursionDepth();
        List<Long> immediateDescendantIds = repository.findIdsByParentId(rootId);
        List<Long> allBranchIds = new ArrayList<>();
        allBranchIds.add(rootId); // Include the root itself!
        allBranchIds.addAll(immediateDescendantIds);
        
        for (Long bId : immediateDescendantIds) {
            this.recursivelyAddBranchIds(allBranchIds, bId, depth);
        }
        return allBranchIds;
    }

    @Override
    public List<Long> getBranchIdsFromRootCode(String rootCode) {
        Optional<ProductCategory> byCode = repository.findByCode(rootCode);
        if (byCode.isEmpty()) {
            return List.of();
        }
        return this.getBranchIdsFromRootId(byCode.get().getId());
    }

    private void recursivelyAddBranches(Collection<ProductCategory> allBranches, ProductCategory branch, int deepnessLeft) {
        if (deepnessLeft > 0) {
            List<ProductCategory> subBranchIds = repository.findByParent(branch);
            for (ProductCategory bId2 : subBranchIds) {
                allBranches.add(bId2);
                this.recursivelyAddBranches(allBranches, bId2, deepnessLeft - 1);
            }
        }
    }

    private void recursivelyAddBranchIds(Collection<Long> branchIds, Long bId, int deepnessLeft) {
        if (deepnessLeft > 0) {
            List<Long> subBranchIds = repository.findIdsByParentId(bId);
            for (Long bId2 : subBranchIds) {
                branchIds.add(bId2);
                this.recursivelyAddBranchIds(branchIds, bId2, deepnessLeft - 1);
            }
        }
    }
}
