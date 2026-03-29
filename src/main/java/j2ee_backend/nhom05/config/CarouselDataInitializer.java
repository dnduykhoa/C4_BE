package j2ee_backend.nhom05.config;

import j2ee_backend.nhom05.model.CarouselSlide;
import j2ee_backend.nhom05.repository.ICarouselSlideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Order(10)
@Profile("seed-carousel")
public class CarouselDataInitializer implements CommandLineRunner {

    @Autowired
    private ICarouselSlideRepository repository;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() > 0) return; // Đã có dữ liệu thì bỏ qua

        CarouselSlide s1 = new CarouselSlide();
        s1.setImage("/images/carousel/5956a801-df12-4ede-b87d-a115f01c19f6.mp4");
        s1.setBadge("Công nghệ hàng đầu");
        s1.setTitle("Thiết bị công nghệ\nchính hãng, giá tốt");
        s1.setSubtitle("Laptop, điện thoại và phụ kiện từ các thương hiệu uy tín — giao hàng nhanh toàn quốc.");
        s1.setButtonText("Mua ngay");
        s1.setButtonLink("/products");
        s1.setDisplayOrder(1);
        s1.setIntervalMs(4000);
        s1.setIsActive(true);
        repository.save(s1);

        CarouselSlide s2 = new CarouselSlide();
        s2.setImage("/images/carousel/fa7c474c-00e6-48fe-96a2-f6176be3fd4f.png");
        s2.setBadge("Ưu đãi hôm nay");
        s2.setTitle("Giảm giá lớn\ncho mùa hè này");
        s2.setSubtitle("Hàng ngàn sản phẩm công nghệ giảm giá sâu — chỉ trong thời gian có hạn.");
        s2.setButtonText("Xem ưu đãi");
        s2.setButtonLink("/products");
        s2.setDisplayOrder(2);
        s2.setIntervalMs(4000);
        s2.setIsActive(true);
        repository.save(s2);

        CarouselSlide s3 = new CarouselSlide();
        s3.setImage("/images/carousel/1ce63a06-3694-48b7-8fce-dad84629ad3d.png");
        s3.setBadge("Mới ra mắt");
        s3.setTitle("Sản phẩm mới\nvừa về kho");
        s3.setSubtitle("Cập nhật những thiết bị công nghệ mới nhất, hiện đại nhất tại TechStore.");
        s3.setButtonText("Khám phá ngay");
        s3.setButtonLink("/products");
        s3.setDisplayOrder(3);
        s3.setIntervalMs(4000);
        s3.setIsActive(true);
        repository.save(s3);
    }
}
