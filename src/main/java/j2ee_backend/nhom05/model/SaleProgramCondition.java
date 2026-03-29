package j2ee_backend.nhom05.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sale_program_conditions")
public class SaleProgramCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_program_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SaleProgram saleProgram;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", length = 30, nullable = false)
    private ConditionType conditionType;

    // Giá trị điều kiện: "MOMO" / "CASH" hoặc số tiền "500000" hoặc số lượng "2"
    @Column(name = "condition_value", length = 200, nullable = false)
    private String conditionValue;

    // Mô tả điều kiện để hiển thị cho người dùng
    @Column(name = "description", columnDefinition = "NVARCHAR(500)")
    private String description;
}
