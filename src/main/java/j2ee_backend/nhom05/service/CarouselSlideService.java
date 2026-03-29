package j2ee_backend.nhom05.service;

import j2ee_backend.nhom05.dto.CarouselSlideRequest;
import j2ee_backend.nhom05.model.CarouselSlide;
import j2ee_backend.nhom05.repository.ICarouselSlideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarouselSlideService {

    @Autowired
    private ICarouselSlideRepository repository;

    // Lấy các slide đang active (dùng cho HomePage)
    public List<CarouselSlide> getActiveSlides() {
        return repository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    // Lấy tất cả slide (dùng cho trang admin)
    public List<CarouselSlide> getAllSlides() {
        return repository.findAllByOrderByDisplayOrderAsc();
    }

    // Tạo slide mới
    public CarouselSlide create(CarouselSlideRequest req) {
        CarouselSlide slide = new CarouselSlide();
        mapRequest(slide, req);
        return repository.save(slide);
    }

    // Cập nhật slide
    public CarouselSlide update(Long id, CarouselSlideRequest req) {
        CarouselSlide slide = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy slide"));
        mapRequest(slide, req);
        return repository.save(slide);
    }

    // Xóa slide
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy slide");
        }
        repository.deleteById(id);
    }

    // Map request vào entity
    private void mapRequest(CarouselSlide slide, CarouselSlideRequest req) {
        if (req.getImage() != null)       slide.setImage(req.getImage());
        if (req.getMediaType() != null)   slide.setMediaType(req.getMediaType());
        if (req.getBadge() != null)       slide.setBadge(req.getBadge());
        if (req.getTitle() != null)       slide.setTitle(req.getTitle());
        if (req.getSubtitle() != null)    slide.setSubtitle(req.getSubtitle());
        if (req.getButtonText() != null)  slide.setButtonText(req.getButtonText());
        if (req.getButtonLink() != null)  slide.setButtonLink(req.getButtonLink());
        if (req.getDisplayOrder() != null) slide.setDisplayOrder(req.getDisplayOrder());
        if (req.getIntervalMs() != null)  slide.setIntervalMs(req.getIntervalMs());
        if (req.getIsActive() != null)    slide.setIsActive(req.getIsActive());
    }
}
