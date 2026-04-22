package org.monostudio.api.services;

import org.monostudio.api.models.BulkOperationResult;
import org.monostudio.api.models.ProductCsvImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Service for CSV import/export and bulk operations on ProductVariants.
 */
public interface VariantsBulkService {

    /**
     * Export all product variants as a CSV file.
     *
     * @param productBarcode Optional product barcode to filter variants
     * @return CSV bytes
     * @throws IOException On export error
     */
    byte[] exportVariants(String productBarcode) throws IOException;

    /**
     * Import product variants from a CSV file.
     * Each row creates one ProductVariant linked to its parent Product.
     *
     * @param file CSV file uploaded by admin
     * @return Import result with success/error counts and per-row errors
     * @throws IOException On file read error
     */
    ProductCsvImportResult importVariants(MultipartFile file) throws IOException;

    /**
     * Activate (publish) multiple variants at once.
     */
    BulkOperationResult bulkActivate(List<Long> ids);

    /**
     * Deactivate multiple variants at once.
     */
    BulkOperationResult bulkDeactivate(List<Long> ids);

    /**
     * Delete multiple variants at once.
     */
    BulkOperationResult bulkDelete(List<Long> ids);

    /**
     * Bulk update multiple variants with selected fields.
     */
    BulkOperationResult bulkUpdate(
        List<Long> ids,
        Integer priceModifier,
        Integer currentStock,
        Boolean active
    );
}