package org.monostudio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;

@Data
@Component
@ConfigurationProperties(prefix = "monostudio.api")
@Validated
public class ApiProperties {
    @Positive
    private Integer itemsPerPage;
    @Positive
    private Integer maxAllowedPageSize;
    @Positive
    private int maxCategoryFetchingRecursionDepth;
    private boolean ableToEditOrdersAfterBeingProcessed;
}
