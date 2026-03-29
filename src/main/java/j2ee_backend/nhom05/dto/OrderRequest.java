package j2ee_backend.nhom05.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
        regexp = "^(\\+84|84|0)(3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])\\d{7}$",
        message = "Số điện thoại Việt Nam không hợp lệ (VD: 0912345678 hoặc +84912345678)"
    )
    private String phone;

    private String email;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;

    private String note;

    // "CASH" | "MOMO"
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;

    // Optional: cho phép checkout theo danh sách items trực tiếp (mua ngay)
    private List<OrderItemRequest> items;

    // Optional: mã voucher muốn áp dụng
    private String voucherCode;
}
