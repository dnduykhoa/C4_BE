package j2ee_backend.nhom05.validator;

import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {
    
    private static final int MIN_LENGTH = 8;
    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String LOWERCASE_PATTERN = ".*[a-z].*";
    private static final String DIGIT_PATTERN = ".*[0-9].*";
    private static final String SPECIAL_CHAR_PATTERN = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*";
    
    /**
     * Validate password theo yêu cầu:
     * - Độ dài tối thiểu 8 ký tự
     * - Ít nhất 1 chữ hoa
     * - Ít nhất 1 chữ thường
     * - Ít nhất 1 chữ số
     * - Ít nhất 1 ký tự đặc biệt
     * 
     * @param password Mật khẩu cần validate
     * @throws RuntimeException nếu mật khẩu không hợp lệ
     */
    public void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new RuntimeException("Mật khẩu phải có ít nhất " + MIN_LENGTH + " ký tự");
        }
        
        if (!password.matches(UPPERCASE_PATTERN)) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 chữ hoa");
        }
        
        if (!password.matches(LOWERCASE_PATTERN)) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 chữ thường");
        }
        
        if (!password.matches(DIGIT_PATTERN)) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 chữ số");
        }
        
        if (!password.matches(SPECIAL_CHAR_PATTERN)) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 ký tự đặc biệt (!@#$%^&*()_+-=[]{}...)");
        }
    }
    
    /**
     * Kiểm tra mật khẩu có hợp lệ không (trả về boolean thay vì throw exception)
     * 
     * @param password Mật khẩu cần kiểm tra
     * @return true nếu hợp lệ, false nếu không hợp lệ
     */
    public boolean isValid(String password) {
        try {
            validate(password);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
