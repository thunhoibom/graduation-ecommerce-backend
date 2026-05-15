package org.monostudio.jpa.services.predicates.impl;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.stereotype.Service;
import org.monostudio.jpa.entities.BlogPostStatus;
import org.monostudio.jpa.entities.QBlogPost;
import org.monostudio.jpa.services.predicates.BlogPostsPredicateService;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class BlogPostsPredicateServiceImpl
    implements BlogPostsPredicateService {

    @Override
    public Predicate parseMap(Map<String, String> parameters) {
        BooleanExpression predicate = QBlogPost.blogPost.isNotNull();

        if (parameters.containsKey("title")) {
            predicate = predicate.and(QBlogPost.blogPost.title.containsIgnoreCase(parameters.get("title")));
        }
        if (parameters.containsKey("status")) {
            predicate = predicate.and(QBlogPost.blogPost.status.eq(BlogPostStatus.valueOf(parameters.get("status"))));
        }
        if (parameters.containsKey("authorId")) {
            predicate = predicate.and(QBlogPost.blogPost.author.id.eq(Long.valueOf(parameters.get("authorId"))));
        }
        if ("true".equalsIgnoreCase(parameters.get("publishedOnly"))) {
            predicate = predicate
                .and(QBlogPost.blogPost.publishedAt.isNotNull())
                .and(QBlogPost.blogPost.publishedAt.loe(LocalDateTime.now()));
        }

        return predicate;
    }
}
