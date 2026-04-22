package org.monostudio.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.monostudio.search.models.ProductDocument;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherCategoryRecommendationPojo {
    @Builder.Default
    private String sectionTitle = "Goi y theo thoi tiet";
    private String category;
    private WeatherContextPojo weatherContext;
    @Builder.Default
    private List<ProductDocument> items = new ArrayList<>();
}
