package org.monostudio.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Result of a bulk operation (publish/unpublish/delete).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationResult {
    /** Number of items successfully processed. */
    private int successCount;
    /** Number of items that failed. */
    private int errorCount;
    /** List of error messages per failed item. Null if no errors. */
    private List<String> errors;
}
