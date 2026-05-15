package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Wraps an ImagePojo with sort order and primary flag for product-level images.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ProductImagePojo {
    private ImagePojo image;
    @Builder.Default
    private Integer sortOrder = 0;
    @Builder.Default
    private Boolean isPrimary = false;
}
