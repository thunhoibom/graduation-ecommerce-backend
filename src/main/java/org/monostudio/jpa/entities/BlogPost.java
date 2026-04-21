package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "blog_posts",
    indexes = {
        @Index(columnList = "post_slug", unique = true),
        @Index(columnList = "post_title")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class BlogPost
    implements DBEntity {
    private static final long serialVersionUID = 20L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id", nullable = false)
    private Long id;

    @Size(max = 200)
    @Column(name = "post_title", nullable = false)
    private String title;

    @Size(max = 200)
    @Column(name = "post_slug", nullable = false, unique = true)
    private String slug;

    @Size(max = 500)
    @Column(name = "post_summary")
    private String summary;

    @Lob
    @Column(name = "post_content", nullable = false)
    private String content;

    @Column(name = "post_thumbnail_url")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_status", nullable = false)
    @Builder.Default
    private BlogPostStatus status = BlogPostStatus.DRAFT;

    @JoinColumn(name = "author_user_id", referencedColumnName = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User author;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Copy constructor for patching.
     */
    public BlogPost(BlogPost source) {
        this.id = source.id;
        this.title = source.title;
        this.slug = source.slug;
        this.summary = source.summary;
        this.content = source.content;
        this.thumbnailUrl = source.thumbnailUrl;
        this.status = source.status;
        this.author = source.author;
        this.publishedAt = source.publishedAt;
        this.createdAt = source.createdAt;
        this.updatedAt = source.updatedAt;
    }
}
