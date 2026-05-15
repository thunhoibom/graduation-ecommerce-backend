package org.monostudio.api.services;

import org.monostudio.api.models.ProductCsvImportResult;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;

/**
 * Service for CSV import/export of products.
 */
public interface ProductsBulkService {

    /**
     * Exports all products as a CSV byte array.
     *
     * @param categoryCode Optional category code to filter products
     * @return CSV file as byte[]
     * @throws IOException On export error
     */
    byte[] exportProducts(String categoryCode) throws IOException;

    /**
     * Imports products from a CSV file.
     *
     * @param file CSV file uploaded by admin
     * @return Import result with success/error counts and per-row error details
     * @throws IOException On file read error
     */
    ProductCsvImportResult importProducts(MultipartFile file) throws IOException;
}
