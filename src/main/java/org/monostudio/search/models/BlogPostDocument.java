package org.monostudio.search.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "blogposts")
public class BlogPostDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard", copyTo = "all")
    private String title;

    @Field(type = FieldType.Keyword)
    private String slug;

    @Field(type = FieldType.Text, analyzer = "standard", copyTo = "all")
    private String summary;

    @Field(type = FieldType.Text, analyzer = "standard", copyTo = "all")
    private String content;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String all;
}
