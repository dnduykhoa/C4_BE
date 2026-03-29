package j2ee_backend.nhom05.validator;

import org.springframework.stereotype.Component;

/**
 * Validator số điện thoại Việt Nam.
 *
 * Các đầu số (3 chữ số sau 0) được hỗ trợ:
 *   Viettel      : 032–039 | 086 | 096 | 097 | 098
 *   Mobifone     : 070 | 076–079 | 089 | 090 | 093
 *   Vinaphone    : 081–085 | 088 | 091 | 094
 *   Vietnamobile : 052 | 056 | 058 | 092
 *   Reddi        : 055
 *   Gmobile      : 059 | 099
 *
 * Định dạng chấp nhận (sau khi bỏ khoảng trắng/dấu gạch):
 *   0XXXXXXXXX   — 10 chữ số, bắt đầu bằng 0
 *   +84XXXXXXXXX — quốc tế (12 ký tự)
 *   84XXXXXXXXX  — quốc tế không có dấu + (11 chữ số)
 */
@Component
public class PhoneValidator {

    /**
     * Regex chuẩn áp dụng sau khi xóa khoảng trắng.
     * Nhóm prefix: (0 | +84 | 84)
     * Nhóm mạng  : 3[2-9] | 5[25689] | 7[06-9] | 8[1-9] | 9[0-9]
     * Phần còn lại: 7 chữ số
     */
    public static final String VN_PHONE_REGEX =
        "^(\\+84|84|0)(3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])\\d{7}$";

    /**
     * Chuẩn hóa số điện thoại:
     *   - Xóa khoảng trắng, dấu gạch ngang
     *   - Chuyển +84XXXXXXXXX / 84XXXXXXXXX → 0XXXXXXXXX
     *
     * @return chuỗi đã chuẩn hóa, hoặc null nếu đầu vào null
     */
    public static String normalize(String phone) {
        if (phone == null) return null;
        String p = phone.replaceAll("[\\s\\-]", "");
        if (p.startsWith("+84")) return "0" + p.substring(3);
        if (p.startsWith("84") && p.length() == 11) return "0" + p.substring(2);
        return p;
    }

    /**
     * Kiểm tra số điện thoại có hợp lệ ở Việt Nam không.
     * Tự động xử lý khoảng trắng và tiền tố +84/84.
     *
     * @return true nếu hợp lệ
     */
    public static boolean isValid(String phone) {
        if (phone == null || phone.isBlank()) return false;
        String stripped = phone.replaceAll("[\\s\\-]", "");
        return stripped.matches(VN_PHONE_REGEX);
    }

    /**
     * Validate và ném RuntimeException nếu không hợp lệ.
     */
    public void validate(String phone) {
        if (!isValid(phone)) {
            throw new RuntimeException(
                "Số điện thoại không hợp lệ. Vui lòng nhập đúng định dạng số điện thoại Việt Nam (VD: 0912345678)");
        }
    }
}
