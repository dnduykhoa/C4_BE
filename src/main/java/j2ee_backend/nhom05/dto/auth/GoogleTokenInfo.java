package j2ee_backend.nhom05.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleTokenInfo {

    private String sub;
    private String email;
    private String aud;
    private String name;
    private String picture;

    @JsonProperty("email_verified")
    private String emailVerified;
}