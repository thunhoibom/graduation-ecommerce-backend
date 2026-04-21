package org.monostudio.jpa.services.patch.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.BlogPostPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.BlogPost;
import org.monostudio.jpa.entities.BlogPostStatus;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.jpa.services.patch.BlogPostsPatchService;

import java.util.Map;

@Service
public class BlogPostsPatchServiceImpl
    implements BlogPostsPatchService {

    private final UsersRepository usersRepository;

    public BlogPostsPatchServiceImpl(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public BlogPost patchExistingEntity(Map<String, Object> changes, BlogPost existing) throws BadInputException {
        BlogPost target = new BlogPost(existing);

        if (changes.containsKey("title")) {
            target.setTitle((String) changes.get("title"));
        }
        if (changes.containsKey("slug")) {
            target.setSlug((String) changes.get("slug"));
        }
        if (changes.containsKey("summary")) {
            target.setSummary((String) changes.get("summary"));
        }
        if (changes.containsKey("content")) {
            target.setContent((String) changes.get("content"));
        }
        if (changes.containsKey("thumbnailUrl")) {
            target.setThumbnailUrl((String) changes.get("thumbnailUrl"));
        }
        if (changes.containsKey("status")) {
            target.setStatus(BlogPostStatus.valueOf((String) changes.get("status")));
        }
        if (changes.containsKey("authorId")) {
            Long authorId = ((Number) changes.get("authorId")).longValue();
            usersRepository.findById(authorId).ifPresent(target::setAuthor);
        }

        return target;
    }

    @Override
    public BlogPost patchExistingEntity(BlogPostPojo changes, BlogPost existing) throws BadInputException {
        BlogPost target = new BlogPost(existing);

        if (changes.getTitle() != null && !StringUtils.isBlank(changes.getTitle())) {
            target.setTitle(changes.getTitle());
        }
        if (changes.getSlug() != null && !StringUtils.isBlank(changes.getSlug())) {
            target.setSlug(changes.getSlug());
        }
        if (changes.getSummary() != null) {
            target.setSummary(changes.getSummary());
        }
        if (changes.getContent() != null && !StringUtils.isBlank(changes.getContent())) {
            target.setContent(changes.getContent());
        }
        if (changes.getThumbnailUrl() != null) {
            target.setThumbnailUrl(changes.getThumbnailUrl());
        }
        if (changes.getStatus() != null) {
            target.setStatus(BlogPostStatus.valueOf(changes.getStatus()));
        }
        if (changes.getAuthorId() != null) {
            usersRepository.findById(changes.getAuthorId()).ifPresent(target::setAuthor);
        }

        return target;
    }
}
