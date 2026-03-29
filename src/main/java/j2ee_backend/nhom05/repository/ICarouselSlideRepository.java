package j2ee_backend.nhom05.repository;

import j2ee_backend.nhom05.model.CarouselSlide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ICarouselSlideRepository extends JpaRepository<CarouselSlide, Long> {

    List<CarouselSlide> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<CarouselSlide> findAllByOrderByDisplayOrderAsc();
}
