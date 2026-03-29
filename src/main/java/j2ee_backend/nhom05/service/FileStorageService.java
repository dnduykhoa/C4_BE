package j2ee_backend.nhom05.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    
    private final Path fileStorageLocation;
    
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Không thể tạo thư mục lưu trữ file!", ex);
        }
    }
    
    /**
     * Upload file và trả về đường dẫn tương đối
     * @param file File cần upload
     * @param subDir Thư mục con (vd: "products", "users")
     * @return Đường dẫn file đã upload (vd: "products/uuid-filename.jpg")
     */
    public String storeFile(MultipartFile file, String subDir) {
        String rawName = file.getOriginalFilename();
        // Chuẩn hóa tên file (xử lý null an toàn)
        String originalFileName = (rawName != null) ? StringUtils.cleanPath(rawName) : "";
        
        try {
            // Kiểm tra tên file có ký tự không hợp lệ không
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Tên file không hợp lệ: " + originalFileName);
            }
            
            // Tạo tên file unique với UUID
            String fileExtension = "";
            if (originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            
            // Tạo thư mục con nếu chưa tồn tại
            Path subDirPath = this.fileStorageLocation.resolve(subDir);
            Files.createDirectories(subDirPath);
            
            // Đường dẫn đầy đủ để lưu file
            Path targetLocation = subDirPath.resolve(newFileName);
            
            // Copy file vào thư mục đích
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Trả về đường dẫn tương đối (để lưu vào DB)
            return subDir + "/" + newFileName;
            
        } catch (IOException ex) {
            throw new RuntimeException("Không thể lưu file " + originalFileName, ex);
        }
    }
    
    /**
     * Xóa file
     * @param filePath Đường dẫn file (vd: "products/uuid-filename.jpg")
     */
    public void deleteFile(String filePath) {
        try {
            Path file = this.fileStorageLocation.resolve(filePath).normalize();
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            throw new RuntimeException("Không thể xóa file " + filePath, ex);
        }
    }
    
    /**
     * Kiểm tra file có phải hình ảnh không
     */
    public boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }
    
    /**
     * Kiểm tra file có phải video không
     */
    public boolean isVideoFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("video/");
    }
}
