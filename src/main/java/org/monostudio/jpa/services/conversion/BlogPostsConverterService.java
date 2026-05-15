package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.BlogPostPojo;
import org.monostudio.jpa.entities.BlogPost;
import org.monostudio.jpa.services.ConverterService;

public interface BlogPostsConverterService
    extends ConverterService<BlogPostPojo, BlogPost> {
}
