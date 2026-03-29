package j2ee_backend.nhom05.dto;

import lombok.Data;

@Data
public class CarouselSlideRequest {
    private String image;
    private String mediaType;
    private String badge;
    private String title;
    private String subtitle;
    private String buttonText;
    private String buttonLink;
    private Integer displayOrder;
    private Integer intervalMs;
    private Boolean isActive;
}
