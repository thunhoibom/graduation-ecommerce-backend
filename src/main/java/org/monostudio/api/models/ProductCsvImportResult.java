package org.monostudio.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Result of a CSV product import operation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCsvImportResult {
    /** Total rows processed (header excluded). */
    private int totalRows;
    /** Number of rows successfully imported. */
    private int successCount;
    /** Number of rows that failed. */
    private int errorCount;
    /** Per-row error messages. Null if no errors. */
    private List<ImportRowError> errors;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportRowError {
        /** Row number in CSV (1-indexed, header is row 0). */
        private int row;
        /** Original row data as CSV string. */
        private String rowData;
        /** Error description. */
        private String error;
    }
}