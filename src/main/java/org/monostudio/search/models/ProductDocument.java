package org.monostudio.search.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products")
@Setting(settingPath = "elasticsearch/settings.json") // Optional: for custom analyzers
public class ProductDocument {

    @Id
    private String id;

    /** Keyword subfield is required for sorting; plain `text` fields cannot be sorted in Elasticsearch. */
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "standard_vietnamese", copyTo = "all"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    private String name;

    @Field(type = FieldType.Keyword)
    private String barcode;

    @Field(type = FieldType.Text, analyzer = "standard_vietnamese", copyTo = "all")
    private String description;

    @Field(type = FieldType.Integer)
    private Integer price;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private List<String> categoryCodes;

    @Field(type = FieldType.Keyword)
    private List<String> weatherTags;

    @Field(type = FieldType.Integer)
    private Integer tempMin;

    @Field(type = FieldType.Integer)
    private Integer tempMax;

    @Field(type = FieldType.Keyword)
    private String status;

    /** Numeric id for stable ES sorting (string {@code id} is not ideal for numeric order). */
    @Field(type = FieldType.Long)
    private Long productNumericId;

    /** Aggregate product-level stock (mirrors JPA public listing filter {@code stockCurrent > 0}). */
    @Field(type = FieldType.Integer)
    private Integer stockCurrent;

    /** Distinct variant colors on this product (lowercase) for facet-style filtering. */
    @Field(type = FieldType.Keyword)
    private List<String> variantColors;

    /** Distinct variant sizes on this product (lowercase) for facet-style filtering. */
    @Field(type = FieldType.Keyword)
    private List<String> variantSizes;

    @Field(type = FieldType.Keyword)
    private String primaryImageUrl;

    @Field(type = FieldType.Text, analyzer = "standard_vietnamese")
    private String all; // For multi-field search
}
