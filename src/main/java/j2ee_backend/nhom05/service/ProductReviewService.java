package j2ee_backend.nhom05.service;

import j2ee_backend.nhom05.dto.ReviewRequest;
import j2ee_backend.nhom05.dto.ReviewResponse;
import j2ee_backend.nhom05.model.*;
import j2ee_backend.nhom05.repository.IOrderItemRepository;
import j2ee_backend.nhom05.repository.IProductReviewRepository;
import j2ee_backend.nhom05.repository.IUserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductReviewService {

    private static final int MAX_IMAGES = 5;
    private static final long MAX_IMAGE_SIZE = 2L * 1024 * 1024; // 2MB

    @Autowired
    private IProductReviewRepository reviewRepository;

    @Autowired
    private IOrderItemRepository orderItemRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Người dùng gửi đánh giá sản phẩm.
     * Điều kiện: đơn hàng phải có trạng thái DELIVERED và người dùng chưa đánh giá item này.
     */
    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request, List<MultipartFile> images) {
        // Lấy order item
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong đơn hàng"));

        // Kiểm tra đơn hàng thuộc về user này
        Order order = orderItem.getOrder();
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền đánh giá sản phẩm này");
        }

        // Kiểm tra đơn hàng đã giao hàng chưa
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Chỉ có thể đánh giá sản phẩm sau khi đơn hàng đã được giao");
        }

        // Kiểm tra đã đánh giá chưa
        if (reviewRepository.existsByUserIdAndOrderItemId(userId, request.getOrderItemId())) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Product product = orderItem.getProduct();
        ProductVariant variant = orderItem.getVariant();

        ProductReview review = new ProductReview();
        review.setUser(user);
        review.setProduct(product);
        review.setVariant(variant);
        review.setOrderItem(orderItem);
        review.setRating(request.getRating());
        review.setComment(request.getComment() != null ? request.getComment().trim() : null);

        // Xử lý ảnh đính kèm
        if (images != null) {
            List<MultipartFile> nonEmpty = images.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .collect(Collectors.toList());

            if (nonEmpty.size() > MAX_IMAGES) {
                throw new RuntimeException("Tối đa " + MAX_IMAGES + " ảnh mỗi đánh giá");
            }

            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : nonEmpty) {
                if (image.getSize() > MAX_IMAGE_SIZE) {
                    throw new RuntimeException("Mỗi ảnh tối đa 2MB");
                }
                if (!fileStorageService.isImageFile(image)) {
                    throw new RuntimeException("Chỉ chấp nhận file ảnh (jpg, png, webp, ...)");
                }
                String path = fileStorageService.storeFile(image, "reviews");
                imageUrls.add("/images/" + path);
            }
            review.setImageUrls(imageUrls);
        }

        ProductReview saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    /**
     * Lấy danh sách đánh giá của một sản phẩm (công khai, chỉ hiển thị chưa bị ẩn).
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProduct(Long productId) {
    return reviewRepository.findByProductIdAndVariantIdIsNullAndHiddenFalseOrderByCreatedAtDesc(productId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách đánh giá của một biến thể sản phẩm (công khai, chỉ hiển thị chưa bị ẩn).
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProductAndVariant(Long productId, Long variantId) {
        return reviewRepository.findByProductIdAndVariantIdAndHiddenFalseOrderByCreatedAtDesc(productId, variantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy điểm trung bình và số lượng đánh giá của sản phẩm (chỉ tính review chưa ẩn, KHÔNG bao gồm variant).
     */
    @Transactional(readOnly = true)
    public ReviewSummary getReviewSummary(Long productId) {
        double avg = reviewRepository.findAverageRatingByProductId(productId);
        long count = reviewRepository.countByProductIdAndVariantIsNullAndHiddenFalse(productId);
        return new ReviewSummary(avg, count);
    }

    /**
     * Lấy điểm trung bình và số lượng đánh giá của một biến thể sản phẩm (chỉ tính review chưa ẩn).
     */
    @Transactional(readOnly = true)
    public ReviewSummary getReviewSummaryByVariant(Long productId, Long variantId) {
        double avg = reviewRepository.findAverageRatingByProductIdAndVariantId(productId, variantId);
        long count = reviewRepository.countByProductIdAndVariantIdAndHiddenFalse(productId, variantId);
        return new ReviewSummary(avg, count);
    }

    /**
     * Người dùng cập nhật đánh giá của mình.
     */
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, Integer rating, String comment, List<MultipartFile> images) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sửa đánh giá này");
        }

        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("Đánh giá sao phải từ 1 đến 5");
        }

        review.setRating(rating);
        review.setComment(comment != null ? comment.trim() : null);

        // Nếu có ảnh mới thì thay thế ảnh cũ
        if (images != null) {
            List<MultipartFile> nonEmpty = images.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .collect(Collectors.toList());

            if (!nonEmpty.isEmpty()) {
                if (nonEmpty.size() > MAX_IMAGES) {
                    throw new RuntimeException("Tối đa " + MAX_IMAGES + " ảnh mỗi đánh giá");
                }
                // Xóa ảnh cũ
                if (review.getImageUrls() != null) {
                    for (String url : review.getImageUrls()) {
                        try {
                            String filePath = url.startsWith("/images/") ? url.substring("/images/".length()) : url;
                            fileStorageService.deleteFile(filePath);
                        } catch (Exception ignored) {}
                    }
                }
                // Lưu ảnh mới
                List<String> imageUrls = new ArrayList<>();
                for (MultipartFile image : nonEmpty) {
                    if (image.getSize() > MAX_IMAGE_SIZE) {
                        throw new RuntimeException("Mỗi ảnh tối đa 2MB");
                    }
                    if (!fileStorageService.isImageFile(image)) {
                        throw new RuntimeException("Chỉ chấp nhận file ảnh (jpg, png, webp, ...)");
                    }
                    String path = fileStorageService.storeFile(image, "reviews");
                    imageUrls.add("/images/" + path);
                }
                review.setImageUrls(imageUrls);
            }
        }

        return toResponse(reviewRepository.save(review));
    }

    /**
     * [ADMIN] Lấy tất cả đánh giá, hỗ trợ lọc theo từ khóa và số sao.
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews(String keyword, Integer rating) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return reviewRepository.findAllWithFilters(kw, rating)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * [ADMIN] Ẩn / hiện một đánh giá.
     */
    @Transactional
    public ReviewResponse toggleHidden(Long id) {
        ProductReview review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));
        review.setHidden(!review.isHidden());
        return toResponse(reviewRepository.save(review));
    }

    /**
     * [ADMIN] Xóa một đánh giá theo ID.
     */
    @Transactional
    public void deleteReview(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy đánh giá");
        }
        reviewRepository.deleteById(id);
    }

    /**
     * [ADMIN] Trả lời một đánh giá. Nếu reply rỗng/null thì xóa phản hồi.
     */
    @Transactional
    public ReviewResponse replyToReview(Long reviewId, String reply) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));
        if (reply == null || reply.isBlank()) {
            review.setReply(null);
            review.setRepliedAt(null);
        } else {
            review.setReply(reply.trim());
            review.setRepliedAt(java.time.LocalDateTime.now());
        }
        return toResponse(reviewRepository.save(review));
    }

    private ReviewResponse toResponse(ProductReview review) {
        String username = review.getUser().getFullName() != null
                ? review.getUser().getFullName()
                : review.getUser().getUsername();
        Long variantId = review.getVariant() != null ? review.getVariant().getId() : null;
        String variantSku = review.getVariant() != null ? review.getVariant().getSku() : null;
        return new ReviewResponse(
                review.getId(),
                review.getUser().getId(),
                username,
                review.getProduct().getId(),
                review.getProduct().getName(),
                variantId,
                variantSku,
                review.getOrderItem().getId(),
                review.getRating(),
                review.getComment(),
                review.getImageUrls(),
                review.isHidden(),
                review.getCreatedAt(),
                review.getReply(),
                review.getRepliedAt());
    }

    public static class ReviewSummary {
        public final double averageRating;
        public final long totalReviews;

        public ReviewSummary(double averageRating, long totalReviews) {
            this.averageRating = averageRating;
            this.totalReviews = totalReviews;
        }
    }
}
