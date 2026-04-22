package org.monostudio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductCatalogConstraintsInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ProductCatalogConstraintsInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public ProductCatalogConstraintsInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureUniqueConstraints() {
        warnIfDuplicate("products", "product_code");
        warnIfDuplicate("product_variants", "variant_sku");
        warnIfDuplicate("product_variants", "variant_barcode");

        createUniqueIndexIfMissing(
            "ux_products_product_code",
            "products",
            "product_code",
            true
        );
        createUniqueIndexIfMissing(
            "ux_product_variants_variant_sku",
            "product_variants",
            "variant_sku",
            true
        );
        // Nullable unique: many NULLs are allowed, non-null values must be unique.
        createUniqueIndexIfMissing(
            "ux_product_variants_variant_barcode",
            "product_variants",
            "variant_barcode",
            false
        );
    }

    private void warnIfDuplicate(String table, String column) {
        String sql = """
            SELECT COUNT(*)
            FROM (
              SELECT %s
              FROM %s
              WHERE %s IS NOT NULL
              GROUP BY %s
              HAVING COUNT(*) > 1
            ) duplicated
            """.formatted(column, table, column, column);

        Integer duplicatedValueCount = jdbcTemplate.queryForObject(sql, Integer.class);
        if (duplicatedValueCount != null && duplicatedValueCount > 0) {
            logger.warn(
                "Detected {} duplicated values on {}.{} before enforcing unique index.",
                duplicatedValueCount, table, column
            );
        }
    }

    private void createUniqueIndexIfMissing(
        String indexName,
        String table,
        String column,
        boolean notNullOnly
    ) {
        String whereClause = notNullOnly ? " WHERE " + column + " IS NOT NULL" : "";
        String sql = "CREATE UNIQUE INDEX IF NOT EXISTS "
            + indexName
            + " ON "
            + table
            + " ("
            + column
            + ")"
            + whereClause;

        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ex) {
            logger.warn(
                "Could not create unique index {} on {}.{}: {}",
                indexName, table, column, ex.getMessage()
            );
        }
    }
}
