package j2ee_backend.nhom05.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum DiscountType {
    PERCENTAGE,   // Giảm theo % (ví dụ: 20 = giảm 20%)
    FIXED_AMOUNT;  // Giảm số tiền cố định (ví dụ: 50000 = giảm 50.000đ)

    @JsonCreator
    public static DiscountType fromValue(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String normalized = rawValue.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        return switch (normalized) {
            case "PERCENT", "PERCENTAGE", "PERCENTAGES" -> PERCENTAGE;
            case "FIXED", "FIXED_AMOUNT", "AMOUNT" -> FIXED_AMOUNT;
            default -> throw new IllegalArgumentException(
                    "Discount type không hợp lệ: " + rawValue + ". Giá trị hợp lệ: PERCENT/PERCENTAGE, FIXED/FIXED_AMOUNT");
        };
    }
}
