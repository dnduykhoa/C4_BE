package j2ee_backend.nhom05.model;

public enum ConditionType {
    PAYMENT_METHOD,   // Điều kiện phương thức thanh toán (value: "MOMO", "CASH")
    MIN_ORDER_AMOUNT, // Điều kiện tổng tiền đơn hàng tối thiểu (value: "500000")
    MIN_QUANTITY      // Điều kiện tổng số lượng sản phẩm tối thiểu trong đơn (value: "2")
}
