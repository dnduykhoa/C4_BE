package j2ee_backend.nhom05.config;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Chạy một lần khi startup để áp dụng các thay đổi schema mà ddl-auto=update không tự xử lý.
 * Mỗi bước được bọc try-catch độc lập, an toàn khi chạy lại nhiều lần.
 */
@Component
@Order(1)
public class SchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationRunner.class);

    private final DataSource dataSource;

    public SchemaMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Xóa ràng buộc UNIQUE(product_id, sku) trên bảng product_variants
        // vì "tên biến thể" (sku) có thể trùng nhau trong cùng 1 sản phẩm.
        dropConstraintIfExists("product_variants", "UKotrdr01rrxy6fms8yuyd06jxx");
        recreateProductStatusCheckConstraint();
    }

    private void dropConstraintIfExists(String tableName, String constraintName) {
        String sql =
            "IF EXISTS (" +
            "  SELECT 1 FROM sys.key_constraints " +
            "  WHERE name = ? AND parent_object_id = OBJECT_ID(?)" +
            ") " +
            "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, constraintName);
            stmt.setString(2, "dbo." + tableName);
            stmt.execute();
            log.info("[SchemaMigration] Đã xóa constraint '{}' trên bảng '{}' (hoặc constraint không tồn tại).",
                constraintName, tableName);
        } catch (Exception e) {
            log.warn("[SchemaMigration] Không thể xóa constraint '{}': {}", constraintName, e.getMessage());
        }
    }

    private void recreateProductStatusCheckConstraint() {
        String sql = """
            DECLARE @constraint_name NVARCHAR(128);
            SELECT @constraint_name = cc.name
            FROM sys.check_constraints cc
            INNER JOIN sys.columns c ON c.object_id = cc.parent_object_id AND c.column_id = cc.parent_column_id
            WHERE cc.parent_object_id = OBJECT_ID('dbo.products')
              AND c.name = 'status';

            IF @constraint_name IS NOT NULL
            BEGIN
                EXEC('ALTER TABLE dbo.products DROP CONSTRAINT [' + @constraint_name + ']');
            END;

            ALTER TABLE dbo.products
            ADD CONSTRAINT CK_products_status
            CHECK (status IN ('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK', 'NEW_ARRIVAL'));
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
            log.info("[SchemaMigration] Đã cập nhật CHECK constraint cho cột products.status.");
        } catch (Exception e) {
            log.warn("[SchemaMigration] Không thể cập nhật CHECK constraint products.status: {}", e.getMessage());
        }
    }
}
