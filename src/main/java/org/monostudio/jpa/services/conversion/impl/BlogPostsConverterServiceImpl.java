package org.monostudio.jpa.services.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.BlogPostPojo;
import org.monostudio.jpa.entities.BlogPost;
import org.monostudio.jpa.entities.BlogPostStatus;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.jpa.services.conversion.BlogPostsConverterService;

@Service
public class BlogPostsConverterServiceImpl
    implements BlogPostsConverterService {

    private final UsersRepository usersRepository;

    @Autowired
    public BlogPostsConverterServiceImpl(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public BlogPostPojo convertToPojo(BlogPost source) {
        BlogPostPojo target = BlogPostPojo.builder()
            .id(source.getId())
            .title(source.getTitle())
            .slug(source.getSlug())
            .summary(source.getSummary())
            .content(source.getContent())
            .thumbnailUrl(source.getThumbnailUrl())
            .status(source.getStatus().name())
            .publishedAt(source.getPublishedAt())
            .createdAt(source.getCreatedAt())
            .updatedAt(source.getUpdatedAt())
            .build();

        if (source.getAuthor() != null) {
            target.setAuthorId(source.getAuthor().getId());
            target.setAuthorName(source.getAuthor().getName());
        }

        return target;
    }

    @Override
    public BlogPost convertToNewEntity(BlogPostPojo source) {
        BlogPost target = BlogPost.builder()
            .title(source.getTitle())
            .slug(source.getSlug())
            .summary(source.getSummary())
            .content(source.getContent())
            .thumbnailUrl(source.getThumbnailUrl())
            .publishedAt(source.getPublishedAt())
            .build();

        if (source.getStatus() != null) {
            target.setStatus(BlogPostStatus.valueOf(source.getStatus()));
        }

        if (source.getAuthorId() != null) {
            usersRepository.findById(source.getAuthorId()).ifPresent(target::setAuthor);
        }

        return target;
    }

    @Override
    public BlogPost applyChangesToExistingEntity(BlogPostPojo source, BlogPost target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
