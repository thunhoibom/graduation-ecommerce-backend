package org.monostudio.search.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexEvent {
    private String entityType; // "PRODUCT", "BLOG_POST"
    private Long entityId;
    private String operation; // "CREATE", "UPDATE", "DELETE"
}
