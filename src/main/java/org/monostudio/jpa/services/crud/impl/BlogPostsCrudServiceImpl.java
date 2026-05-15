package org.monostudio.jpa.services.crud.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.BlogPostPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.BlogPost;
import org.monostudio.jpa.entities.BlogPostStatus;
import org.monostudio.jpa.repositories.BlogPostsRepository;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.jpa.services.conversion.BlogPostsConverterService;
import org.monostudio.jpa.services.crud.BlogPostsCrudService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.patch.BlogPostsPatchService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class BlogPostsCrudServiceImpl
    extends CrudGenericService<BlogPostPojo, BlogPost>
    implements BlogPostsCrudService {

    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9-]");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    private final BlogPostsRepository blogPostsRepository;
    private final UsersRepository usersRepository;
    private final org.monostudio.search.kafka.IndexEventProducer indexEventProducer;

    @Autowired
    public BlogPostsCrudServiceImpl(
        BlogPostsRepository blogPostsRepository,
        UsersRepository usersRepository,
        BlogPostsConverterService converterService,
        BlogPostsPatchService patchService,
        org.monostudio.search.kafka.IndexEventProducer indexEventProducer
    ) {
        super(blogPostsRepository, converterService, patchService);
        this.blogPostsRepository = blogPostsRepository;
        this.usersRepository = usersRepository;
        this.indexEventProducer = indexEventProducer;
    }

    @Override
    public BlogPostPojo create(BlogPostPojo input) throws BadInputException, jakarta.persistence.EntityExistsException {
        prepareForPersist(input, null);
        BlogPostPojo pojo = super.create(input);
        indexEventProducer.sendIndexEvent("BLOG_POST", pojo.getId(), "CREATE");
        return pojo;
    }

    @Override
    public Optional<BlogPostPojo> update(BlogPostPojo input, Long id) throws jakarta.persistence.EntityNotFoundException, BadInputException {
        prepareForPersist(input, id);
        Optional<BlogPostPojo> pojo = super.update(input, id);
        pojo.ifPresent(p -> indexEventProducer.sendIndexEvent("BLOG_POST", p.getId(), "UPDATE"));
        return pojo;
    }

    @Override
    public Optional<BlogPostPojo> partialUpdate(java.util.Map<String, Object> changes, Long id) {
        normalizePatchPayload(changes, id);
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

    private void prepareForPersist(BlogPostPojo input, Long existingId) throws BadInputException {
        assignCurrentAuthorIfMissing(input);
        if (input.getSlug() == null || input.getSlug().isBlank()) {
            input.setSlug(input.getTitle());
        }
        input.setSlug(ensureUniqueSlug(slugify(input.getSlug()), existingId));
        input.setStatus(normalizeStatus(input.getStatus()));

        if (BlogPostStatus.PUBLISHED.name().equals(input.getStatus())) {
            if (input.getPublishedAt() == null) {
                input.setPublishedAt(LocalDateTime.now());
            }
        } else {
            input.setPublishedAt(null);
        }
    }

    private void assignCurrentAuthorIfMissing(BlogPostPojo input) {
        if (input.getAuthorId() != null) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            return;
        }

        usersRepository.findByName(username).ifPresent(user -> input.setAuthorId(user.getId()));
    }

    private void normalizePatchPayload(java.util.Map<String, Object> changes, Long existingId) {
        if (changes == null) {
            return;
        }

        if (changes.containsKey("slug") || changes.containsKey("title")) {
            String candidateSlug = (String) changes.get("slug");
            if (candidateSlug == null || candidateSlug.isBlank()) {
                candidateSlug = (String) changes.get("title");
            }
            if (candidateSlug != null && !candidateSlug.isBlank()) {
                changes.put("slug", ensureUniqueSlug(slugify(candidateSlug), existingId));
            }
        }

        if (changes.containsKey("status")) {
            String status = normalizeStatus(String.valueOf(changes.get("status")));
            changes.put("status", status);
            if (BlogPostStatus.PUBLISHED.name().equals(status)) {
                if (!changes.containsKey("publishedAt") || changes.get("publishedAt") == null) {
                    changes.put("publishedAt", LocalDateTime.now());
                }
            } else {
                changes.put("publishedAt", null);
            }
        }
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return BlogPostStatus.DRAFT.name();
        }
        return status.trim().toUpperCase();
    }

    private String ensureUniqueSlug(String baseSlug, Long existingId) {
        String normalizedBase = (baseSlug == null || baseSlug.isBlank()) ? "post" : baseSlug;
        String candidate = normalizedBase;
        int suffix = 1;
        while (true) {
            Optional<BlogPost> existing = blogPostsRepository.findBySlug(candidate);
            if (existing.isEmpty() || (existingId != null && existing.get().getId().equals(existingId))) {
                return candidate;
            }
            candidate = normalizedBase + "-" + suffix++;
        }
    }

    private static String slugify(String source) {
        if (source == null || source.isBlank()) {
            return "post";
        }
        String normalized = java.text.Normalizer.normalize(source, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase()
            .replace("đ", "d")
            .replaceAll("\\s+", "-")
            .trim();
        normalized = NON_SLUG_CHARS.matcher(normalized).replaceAll("-");
        normalized = MULTI_DASH.matcher(normalized).replaceAll("-");
        normalized = normalized.replaceAll("^-|-$", "");
        return normalized.isBlank() ? "post" : normalized;
    }
}
