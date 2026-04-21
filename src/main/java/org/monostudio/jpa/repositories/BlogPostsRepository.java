package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.BlogPost;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface BlogPostsRepository
    extends Repository<BlogPost> {

    Optional<BlogPost> findBySlug(String slug);
}
