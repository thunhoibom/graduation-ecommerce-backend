package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ProductReviewReplyPojo {
    private Long id;
    
    @NotBlank
    private String body;
    
    /** Read-only: resolved author name (User or Customer) */
    private String authorName;
    
    /** Read-only: whether author is staff */
    private Boolean isStaff;
    
    private LocalDateTime createdAt;
}
