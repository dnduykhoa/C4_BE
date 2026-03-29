package j2ee_backend.nhom05.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentDeadlineScheduler {

    @Autowired
    private OrderService orderService;

    /**
     * Mỗi phút quét và tự huỷ các đơn online quá hạn thanh toán.
     */
    @Scheduled(fixedDelay = 60000)
    public void expireUnpaidOrdersTask() {
        orderService.expireUnpaidOrders();
    }
}
