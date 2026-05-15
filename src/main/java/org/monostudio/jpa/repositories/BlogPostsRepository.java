package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.BlogPost;
import org.monostudio.jpa.entities.BlogPostStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface BlogPostsRepository
    extends Repository<BlogPost> {

    Optional<BlogPost> findBySlug(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    List<BlogPost> findTop6ByStatusAndPublishedAtLessThanEqualAndIdNotOrderByPublishedAtDesc(
        BlogPostStatus status,
        LocalDateTime publishedAt,
        Long excludedId
    );
}
