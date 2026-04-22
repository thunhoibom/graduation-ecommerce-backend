package org.monostudio.api.services.impl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.monostudio.api.models.BulkOperationResult;
import org.monostudio.api.models.ProductCsvImportResult;
import org.monostudio.api.services.VariantsBulkService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.repositories.ProductVariantsRepository;
import org.monostudio.jpa.repositories.ProductsRepository;
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
public class VariantsBulkServiceImpl implements VariantsBulkService {

    private static final String[] CSV_HEADERS = {
        "sku", "productBarcode", "size", "color", "attributes",
        "priceModifier", "currentStock", "criticalStock", "barcode", "active"
    };

    private final ProductVariantsRepository variantsRepository;
    private final ProductsRepository productsRepository;

    public VariantsBulkServiceImpl(
        ProductVariantsRepository variantsRepository,
        ProductsRepository productsRepository
    ) {
        this.variantsRepository = variantsRepository;
        this.productsRepository = productsRepository;
    }

    // ── Export ────────────────────────────────────────────────────────────────

    @Override
    public byte[] exportVariants(String productBarcode) throws IOException {
        StringWriter out = new StringWriter();

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader(CSV_HEADERS)
            .build();

        List<ProductVariant> variants;
        if (productBarcode != null && !productBarcode.isBlank()) {
            variants = variantsRepository.findAll().stream()
                .filter(v -> productBarcode.equals(v.getProduct() != null ? v.getProduct().getBarcode() : null))
                .toList();
        } else {
            variants = variantsRepository.findAll();
        }

        try (CSVPrinter printer = new CSVPrinter(out, csvFormat)) {
            for (ProductVariant v : variants) {
                String prodBarcode = v.getProduct() != null ? v.getProduct().getBarcode() : "";
                printer.printRecord(
                    v.getSku(),
                    prodBarcode,
                    v.getSize(),
                    v.getColor(),
                    v.getAttributes(),
                    v.getPriceModifier(),
                    v.getStockCurrent(),
                    v.getStockCritical(),
                    v.getBarcode(),
                    v.isActive()
                );
            }
        }

        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Import ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductCsvImportResult importVariants(MultipartFile file) throws IOException {
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

        List<ProductVariant> toSave = new ArrayList<>();
        int rowNum = 0; // header = 0, data starts at 1

        for (CSVRecord record : records) {
            rowNum++;
            String rowData = record.toString();

            try {
                ProductVariant variant = parseRow(record, rowNum, rowData, errors);
                if (variant != null) {
                    toSave.add(variant);
                }
            } catch (Exception e) {
                errors.add(new ProductCsvImportResult.ImportRowError(
                    rowNum, rowData, e.getMessage()));
            }
        }

        if (!toSave.isEmpty()) {
            // saveAll handles INSERT (no id) or UPDATE (has id) per entity
            List<ProductVariant> saved = variantsRepository.saveAll(toSave);
            successCount = saved.size();
        }

        return ProductCsvImportResult.builder()
            .totalRows(rowNum)
            .successCount(successCount)
            .errorCount(errors.size())
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }

    private ProductVariant parseRow(
        CSVRecord record,
        int rowNum,
        String rowData,
        List<ProductCsvImportResult.ImportRowError> errors
    ) throws BadInputException {
        String sku = getField(record, "sku", true, rowNum, rowData, errors);
        String productBarcode = getField(record, "productBarcode", true, rowNum, rowData, errors);
        String size = getField(record, "size", true, rowNum, rowData, errors);

        // ── Resolve parent product ──
        Product parentProduct = productsRepository.findByBarcode(productBarcode.trim())
            .orElseThrow(() -> new BadInputException(
                "Product not found with barcode: '" + productBarcode + "' (row " + rowNum + ")"));

        // ── Check for existing variant by SKU (upsert) ──
        Optional<ProductVariant> existing = variantsRepository.findBySku(sku.trim());
        ProductVariant variant = existing.orElseGet(() ->
            ProductVariant.builder()
                .sku(sku.trim())
                .size(size.trim())
                .priceModifier(0)
                .stockCurrent(0)
                .stockCritical(0)
                .stockReserved(0)
                .active(true)
                .build()
        );

        variant.setProduct(parentProduct);

        // Optional fields
        if (record.isMapped("color")) {
            String color = record.get("color");
            if (color != null && !color.isBlank()) {
                variant.setColor(color.trim());
            }
        }
        if (record.isMapped("attributes")) {
            String attrs = record.get("attributes");
            if (attrs != null && !attrs.isBlank()) {
                variant.setAttributes(attrs.trim());
            }
        }
        if (record.isMapped("priceModifier")) {
            String pm = record.get("priceModifier");
            if (pm != null && !pm.isBlank()) {
                try {
                    variant.setPriceModifier(Integer.parseInt(pm.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (record.isMapped("currentStock")) {
            String stock = record.get("currentStock");
            if (stock != null && !stock.isBlank()) {
                try {
                    variant.setStockCurrent(Integer.parseInt(stock.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (record.isMapped("criticalStock")) {
            String crit = record.get("criticalStock");
            if (crit != null && !crit.isBlank()) {
                try {
                    variant.setStockCritical(Integer.parseInt(crit.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (record.isMapped("barcode")) {
            String barcode = record.get("barcode");
            if (barcode != null && !barcode.isBlank()) {
                variant.setBarcode(barcode.trim());
            }
        }
        if (record.isMapped("active")) {
            String activeStr = record.get("active");
            if (activeStr != null && !activeStr.isBlank()) {
                variant.setActive(
                    "true".equalsIgnoreCase(activeStr.trim())
                    || "1".equals(activeStr.trim())
                    || "yes".equalsIgnoreCase(activeStr.trim())
                );
            }
        }

        return variant;
    }

    // ── Bulk ops ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BulkOperationResult bulkActivate(List<Long> ids) {
        return bulkUpdateActive(ids, true);
    }

    @Override
    @Transactional
    public BulkOperationResult bulkDeactivate(List<Long> ids) {
        return bulkUpdateActive(ids, false);
    }

    private BulkOperationResult bulkUpdateActive(List<Long> ids, boolean active) {
        if (ids == null || ids.isEmpty()) {
            return BulkOperationResult.builder()
                .successCount(0).errorCount(0).build();
        }
        List<String> errors = new ArrayList<>();
        int success = 0;
        for (Long id : ids) {
            try {
                variantsRepository.findById(id).ifPresent(v -> {
                    v.setActive(active);
                    variantsRepository.save(v);
                });
                success++;
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }
        return BulkOperationResult.builder()
            .successCount(success)
            .errorCount(errors.size())
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }

    @Override
    @Transactional
    public BulkOperationResult bulkDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return BulkOperationResult.builder()
                .successCount(0).errorCount(0).build();
        }
        List<String> errors = new ArrayList<>();
        int success = 0;
        for (Long id : ids) {
            try {
                variantsRepository.deleteById(id);
                success++;
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }
        return BulkOperationResult.builder()
            .successCount(success)
            .errorCount(errors.size())
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }

    @Override
    @Transactional
    public BulkOperationResult bulkUpdate(
        List<Long> ids,
        Integer priceModifier,
        Integer currentStock,
        Boolean active
    ) {
        if (ids == null || ids.isEmpty()) {
            return BulkOperationResult.builder()
                .successCount(0).errorCount(0).build();
        }

        if (priceModifier == null && currentStock == null && active == null) {
            return BulkOperationResult.builder()
                .successCount(0)
                .errorCount(1)
                .errors(List.of("At least one field must be provided: priceModifier, currentStock, active"))
                .build();
        }

        List<String> errors = new ArrayList<>();
        int success = 0;

        for (Long id : ids) {
            try {
                Optional<ProductVariant> optionalVariant = variantsRepository.findById(id);
                if (optionalVariant.isEmpty()) {
                    errors.add("ID " + id + ": variant not found");
                    continue;
                }

                ProductVariant variant = optionalVariant.get();
                if (priceModifier != null) {
                    variant.setPriceModifier(priceModifier);
                }
                if (currentStock != null) {
                    variant.setStockCurrent(currentStock);
                }
                if (active != null) {
                    variant.setActive(active);
                }
                variantsRepository.save(variant);
                success++;
            } catch (Exception e) {
                errors.add("ID " + id + ": " + e.getMessage());
            }
        }

        return BulkOperationResult.builder()
            .successCount(success)
            .errorCount(errors.size())
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }

    // ── Field helpers ─────────────────────────────────────────────────────────

    private String getField(
        CSVRecord record, String fieldName, boolean required,
        int rowNum, String rowData,
        List<ProductCsvImportResult.ImportRowError> errors
    ) throws BadInputException {
        if (!record.isMapped(fieldName)) {
            if (required) throw new BadInputException("Missing required field: " + fieldName);
            return "";
        }
        String value = record.get(fieldName);
        if (required && (value == null || value.isBlank())) {
            throw new BadInputException("Empty required field: " + fieldName);
        }
        return value != null ? value : "";
    }
}