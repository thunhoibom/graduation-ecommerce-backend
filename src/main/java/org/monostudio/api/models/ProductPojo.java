package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ProductPojo {
    @NotBlank
    private String name;
    @NotBlank
    private String barcode;
    @JsonInclude(NON_EMPTY)
    private String description;
    @NotNull
    private Integer price;
    private Integer currentStock;
    private Integer criticalStock;
    private ProductCategoryPojo category;
    private Collection<ImagePojo> images;
}
