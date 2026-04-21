package org.monostudio.api.services.impl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.monostudio.api.models.ProductCsvImportResult;
import org.monostudio.api.services.ProductsBulkService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.entities.ProductStatus;
import org.monostudio.jpa.repositories.ProductImagesRepository;
import org.monostudio.jpa.repositories.ProductsCategoriesRepository;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.search.kafka.IndexEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductsBulkServiceImpl implements ProductsBulkService {

    // CSV column headers for export and import
    private static final String[] CSV_HEADERS = {
        "barcode", "name", "description", "price", "currentStock",
        "criticalStock", "categoryCode", "status"
    };

    private final ProductsRepository productsRepository;
    private final ProductsCategoriesRepository categoriesRepository;
    private final ProductImagesRepository productImagesRepository;
    private final IndexEventProducer indexEventProducer;

    public ProductsBulkServiceImpl(
        ProductsRepository productsRepository,
        ProductsCategoriesRepository categoriesRepository,
        ProductImagesRepository productImagesRepository,
        IndexEventProducer indexEventProducer
    ) {
        this.productsRepository = productsRepository;
        this.categoriesRepository = categoriesRepository;
        this.productImagesRepository = productImagesRepository;
        this.indexEventProducer = indexEventProducer;
    }

    @Override
    public byte[] exportProducts(String categoryCode) throws IOException {
        StringWriter out = new StringWriter();

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader(CSV_HEADERS)
            .build();

        List<Product> products;
        if (categoryCode != null && !categoryCode.isBlank()) {
            products = productsRepository.findAll().stream()
                .filter(p -> categoryCode.equals(
                    p.getProductCategory() != null ? p.getProductCategory().getCode() : null))
                .toList();
        } else {
            products = productsRepository.findAll();
        }

        try (CSVPrinter printer = new CSVPrinter(out, csvFormat)) {
            for (Product product : products) {
                String catCode = product.getProductCategory() != null
                    ? product.getProductCategory().getCode()
                    : "";
                String statusVal = product.getStatus() != null
                    ? product.getStatus().name()
                    : ProductStatus.DRAFT.name();

                printer.printRecord(
                    product.getBarcode(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getStockCurrent(),
                    product.getStockCritical(),
                    catCode,
                    statusVal
                );
            }
        }

        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    public ProductCsvImportResult importProducts(MultipartFile file) throws IOException {
        List<ProductCsvImportResult.ImportRowError> errors = new ArrayList<>();
        int successCount = 0;

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        Iterable<CSVRecord> records;

        try {
            records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim()
                .parse(new StringReader(content));
        } catch (Exception e) {
            throw new IOException("Invalid CSV format: " + e.getMessage(), e);
        }

        List<Product> toSave = new ArrayList<>();
        List<ImportRowContext> rowContexts = new ArrayList<>();
        int rowNum = 1; // header is row 0, data starts at 1

        for (CSVRecord record : records) {
            rowNum++;
            String rowData = record.toString();

            try {
                Product product = parseRow(record, rowNum, rowData, errors);
                if (product != null) {
                    toSave.add(product);
                    rowContexts.add(new ImportRowContext(rowNum, rowData, null));
                }
            } catch (Exception e) {
                errors.add(new ProductCsvImportResult.ImportRowError(rowNum, rowData, e.getMessage()));
            }
        }

        // Batch save all valid products
        if (!toSave.isEmpty()) {
            List<Product> saved = productsRepository.saveAll(toSave);
            successCount = saved.size();
            
            // Trigger Kafka events for all imported products
            saved.forEach(product -> 
                indexEventProducer.sendIndexEvent("PRODUCT", product.getId(), "UPDATE")
            );
        }

        return ProductCsvImportResult.builder()
            .totalRows(rowNum - 1) // -1 for header row
            .successCount(successCount)
            .errorCount(errors.size())
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }

    private Product parseRow(
        CSVRecord record,
        int rowNum,
        String rowData,
        List<ProductCsvImportResult.ImportRowError> errors
    ) throws BadInputException {
        String barcode = getField(record, "barcode", true, rowNum, rowData, errors);
        String name = getField(record, "name", true, rowNum, rowData, errors);
        String priceStr = getField(record, "price", true, rowNum, rowData, errors);

        int price;
        try {
            price = Integer.parseInt(priceStr.trim());
        } catch (NumberFormatException e) {
            throw new BadInputException("Invalid price: '" + priceStr + "'. Must be an integer (VND, no decimals).");
        }

        Product product = Product.builder()
            .name(name.trim())
            .barcode(barcode.trim())
            .price(price)
            .status(ProductStatus.DRAFT)
            .stockCurrent(0)
            .stockCritical(0)
            .build();

        // Optional fields
        if (record.isMapped("description")) {
            String desc = record.get("description");
            if (desc != null && !desc.isBlank()) {
                product.setDescription(desc.trim());
            }
        }

        if (record.isMapped("currentStock")) {
            String stockStr = record.get("currentStock");
            if (stockStr != null && !stockStr.isBlank()) {
                try {
                    product.setStockCurrent(Integer.parseInt(stockStr.trim()));
                } catch (NumberFormatException ignored) {
                    // Use default 0
                }
            }
        }

        if (record.isMapped("criticalStock")) {
            String critStr = record.get("criticalStock");
            if (critStr != null && !critStr.isBlank()) {
                try {
                    product.setStockCritical(Integer.parseInt(critStr.trim()));
                } catch (NumberFormatException ignored) {
                    // Use default 0
                }
            }
        }

        if (record.isMapped("categoryCode")) {
            String catCode = record.get("categoryCode");
            if (catCode != null && !catCode.isBlank()) {
                Optional<ProductCategory> cat = categoriesRepository.findByCode(catCode.trim());
                cat.ifPresent(product::setProductCategory);
            }
        }

        if (record.isMapped("status")) {
            String statusStr = record.get("status");
            if (statusStr != null && !statusStr.isBlank()) {
                try {
                    product.setStatus(ProductStatus.valueOf(statusStr.trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    // Use default DRAFT
                }
            }
        }

        return product;
    }

    /**
     * Safely reads a required field, throwing BadInputException if missing.
     */
    private String getField(
        CSVRecord record,
        String fieldName,
        boolean required,
        int rowNum,
        String rowData,
        List<ProductCsvImportResult.ImportRowError> errors
    ) throws BadInputException {
        if (!record.isMapped(fieldName)) {
            if (required) {
                throw new BadInputException("Missing required field: " + fieldName);
            }
            return "";
        }
        String value = record.get(fieldName);
        if (required && (value == null || value.isBlank())) {
            throw new BadInputException("Empty required field: " + fieldName);
        }
        return value != null ? value : "";
    }

    private static class ImportRowContext {
        final int row;
        final String rowData;
        final String error;

        ImportRowContext(int row, String rowData, String error) {
            this.row = row;
            this.rowData = rowData;
            this.error = error;
        }
    }
}
