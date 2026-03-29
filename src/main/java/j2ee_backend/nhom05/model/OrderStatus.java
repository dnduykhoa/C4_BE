package j2ee_backend.nhom05.model;

public enum OrderStatus {
    PENDING,      // Chờ xác nhận
    CONFIRMED,    // Đã xác nhận
    SHIPPING,     // Đang giao hàng
    DELIVERED,    // Đã giao hàng
    CANCELLED     // Đã huỷ
}
