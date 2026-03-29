package j2ee_backend.nhom05.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProductLifecycleScheduler {

    @Autowired
    private ProductService productService;

    @Autowired
    private PreorderRequestService preorderRequestService;

    @Scheduled(fixedDelay = 60000)
    public void syncProductLifecycle() {
        preorderRequestService.processPendingNotifications();
        productService.expireNewArrivalProducts();
    }
}