package org.monostudio.jpa.services.crud.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.BlogPostPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.BlogPost;
import org.monostudio.jpa.repositories.BlogPostsRepository;
import org.monostudio.jpa.services.conversion.BlogPostsConverterService;
import org.monostudio.jpa.services.crud.BlogPostsCrudService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.patch.BlogPostsPatchService;

import java.util.Optional;

@Service
public class BlogPostsCrudServiceImpl
    extends CrudGenericService<BlogPostPojo, BlogPost>
    implements BlogPostsCrudService {

    private final BlogPostsRepository blogPostsRepository;
    private final org.monostudio.search.kafka.IndexEventProducer indexEventProducer;

    @Autowired
    public BlogPostsCrudServiceImpl(
        BlogPostsRepository blogPostsRepository,
        BlogPostsConverterService converterService,
        BlogPostsPatchService patchService,
        org.monostudio.search.kafka.IndexEventProducer indexEventProducer
    ) {
        super(blogPostsRepository, converterService, patchService);
        this.blogPostsRepository = blogPostsRepository;
        this.indexEventProducer = indexEventProducer;
    }

    @Override
    public BlogPostPojo create(BlogPostPojo input) throws BadInputException, jakarta.persistence.EntityExistsException {
        BlogPostPojo pojo = super.create(input);
        indexEventProducer.sendIndexEvent("BLOG_POST", pojo.getId(), "CREATE");
        return pojo;
    }

    @Override
    public Optional<BlogPostPojo> update(BlogPostPojo input, Long id) throws jakarta.persistence.EntityNotFoundException, BadInputException {
        Optional<BlogPostPojo> pojo = super.update(input, id);
        pojo.ifPresent(p -> indexEventProducer.sendIndexEvent("BLOG_POST", p.getId(), "UPDATE"));
        return pojo;
    }

    @Override
    public Optional<BlogPostPojo> partialUpdate(java.util.Map<String, Object> changes, Long id) {
        Optional<BlogPostPojo> pojo = super.partialUpdate(changes, id);
        pojo.ifPresent(p -> indexEventProducer.sendIndexEvent("BLOG_POST", p.getId(), "UPDATE"));
        return pojo;
    }

    @Override
    public void delete(Long id) throws jakarta.persistence.EntityNotFoundException {
        super.delete(id);
        indexEventProducer.sendIndexEvent("BLOG_POST", id, "DELETE");
    }

    @Override
    public Optional<BlogPost> getExisting(BlogPostPojo input) throws BadInputException {
        if (input.getSlug() != null) {
            return blogPostsRepository.findBySlug(input.getSlug());
        }
        return Optional.empty();
    }
}
