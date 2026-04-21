package org.monostudio.jpa.sortspecs;

import com.querydsl.core.types.OrderSpecifier;
import org.monostudio.jpa.entities.QBlogPost;

import java.util.Map;

public class BlogPostsSortSpec {
    public static final Map<String, OrderSpecifier<?>> ORDER_SPEC_MAP = Map.of(
        "id", QBlogPost.blogPost.id.asc(),
        "title", QBlogPost.blogPost.title.asc(),
        "createdAt", QBlogPost.blogPost.createdAt.desc(),
        "publishedAt", QBlogPost.blogPost.publishedAt.desc()
    );
}
