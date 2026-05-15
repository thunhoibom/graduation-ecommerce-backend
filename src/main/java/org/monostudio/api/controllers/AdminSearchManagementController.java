package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.monostudio.jpa.repositories.BlogPostsRepository;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.search.kafka.IndexEventProducer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data/search")
@Tag(name = "Search Management")
@Slf4j
@RequiredArgsConstructor
public class AdminSearchManagementController {

    private final ProductsRepository productsRepository;
    private final BlogPostsRepository blogPostsRepository;
    private final IndexEventProducer indexEventProducer;

    @PostMapping("/reindex-all")
    @Operation(summary = "Reindex all products and blog posts into Elasticsearch via Kafka")
    @PreAuthorize("hasAuthority('products:update')")
    public String reindexAll() {
        log.info("Starting manual reindexing of all entities...");

        productsRepository.findAll().forEach(product -> {
            indexEventProducer.sendIndexEvent("PRODUCT", product.getId(), "UPDATE");
        });

        blogPostsRepository.findAll().forEach(post -> {
            indexEventProducer.sendIndexEvent("BLOG_POST", post.getId(), "UPDATE");
        });

        return "Reindexing triggered for all entities.";
    }
}
