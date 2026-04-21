package org.monostudio.jpa.services.crud;

import org.monostudio.api.models.BlogPostPojo;
import org.monostudio.jpa.entities.BlogPost;
import org.monostudio.jpa.services.CrudService;

public interface BlogPostsCrudService
    extends CrudService<BlogPostPojo, BlogPost> {
}
