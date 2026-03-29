package j2ee_backend.nhom05.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PreorderRegistrationRequest {

    @NotNull(message = "Thiếu productId")
    private Long productId;

    private Long variantId;

    @NotBlank(message = "Họ tên không được để trống")
    private String customerName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotNull(message = "Số lượng mong muốn không được để trống")
    @Min(value = 1, message = "Số lượng mong muốn phải lớn hơn 0")
    private Integer desiredQuantity;
}