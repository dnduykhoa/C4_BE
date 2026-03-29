package j2ee_backend.nhom05.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "two_factor_codes")
public class TwoFactorCode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "email_or_phone", nullable = false)
    private String emailOrPhone;
    
    @Column(name = "code", nullable = false)
    private String code;
    
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(name = "used", nullable = false)
    private boolean used;
}
