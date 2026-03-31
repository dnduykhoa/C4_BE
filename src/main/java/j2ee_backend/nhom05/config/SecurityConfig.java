package j2ee_backend.nhom05.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/api/vnpay/**",
                    "/api/momo/**",
                    "/api/sse/**",
                    "/images/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/preorders").permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/products/pc-builder/options",
                    "/api/products/pc-builder/summary",
                    "/api/products/pc-builder/checkout-preview"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/carousel").permitAll()

                .requestMatchers(HttpMethod.POST,
                    "/api/products/add",
                    "/api/products/*/media/upload",
                    "/api/products/*/variants/add",
                    "/api/products/*/variants/*/media/upload",
                    "/api/products/*/specifications/add"
                ).hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "STAFF", "ROLE_STAFF")
                .requestMatchers(HttpMethod.PUT,
                    "/api/products/update/*",
                    "/api/products/*/media/*/set-primary",
                    "/api/products/*/variants/update/*",
                    "/api/products/*/variants/*/media/*/set-primary",
                    "/api/products/*/specifications/update/*"
                ).hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "STAFF", "ROLE_STAFF")
                .requestMatchers(HttpMethod.PATCH,
                    "/api/products/*/toggle-active",
                    "/api/products/*/out-of-stock",
                    "/api/products/*/new-arrival",
                    "/api/products/*/restore"
                ).hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "STAFF", "ROLE_STAFF")
                .requestMatchers(HttpMethod.DELETE,
                    "/api/products/delete/*",
                    "/api/products/*/media/*",
                    "/api/products/*/variants/delete/*",
                    "/api/products/*/variants/*/media/*",
                    "/api/products/*/specifications/delete/*",
                    "/api/products/*/specifications/clear"
                ).hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "STAFF", "ROLE_STAFF")

                .requestMatchers(HttpMethod.POST,
                    "/api/categories/add",
                    "/api/brands/add",
                    "/api/attribute-groups/add",
                    "/api/attribute-definitions/add",
                    "/api/category-attributes/assign",
                    "/api/carousel/upload",
                    "/api/carousel",
                    "/api/sale-programs",
                    "/api/vouchers"
                ).hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")
                .requestMatchers(HttpMethod.PUT,
                    "/api/categories/update/*",
                    "/api/brands/update/*",
                    "/api/attribute-groups/update/*",
                    "/api/attribute-definitions/update/*",
                    "/api/category-attributes/update/*",
                    "/api/carousel/*",
                    "/api/sale-programs/*",
                    "/api/vouchers/*"
                ).hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")
                .requestMatchers(HttpMethod.PATCH,
                    "/api/sale-programs/*",
                    "/api/vouchers/*"
                ).hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")
                .requestMatchers(HttpMethod.DELETE,
                    "/api/categories/delete/*",
                    "/api/brands/delete/*",
                    "/api/attribute-groups/delete/*",
                    "/api/attribute-definitions/delete/*",
                    "/api/category-attributes/remove",
                    "/api/category-attributes/remove/*",
                    "/api/carousel/*",
                    "/api/sale-programs/*",
                    "/api/vouchers/*",
                    "/api/vouchers/delete/*"
                ).hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")

                .requestMatchers(HttpMethod.GET, "/api/orders").hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "STAFF", "ROLE_STAFF")
                .requestMatchers(HttpMethod.PATCH, "/api/orders/*/status").hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "STAFF", "ROLE_STAFF")
                .requestMatchers(HttpMethod.GET, "/api/orders/my").hasAnyAuthority("USER", "ROLE_USER")
                .requestMatchers(HttpMethod.GET, "/api/admin/trusted-devices").hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "STAFF", "ROLE_STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/trusted-devices/*").hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "STAFF", "ROLE_STAFF")
                .requestMatchers(HttpMethod.GET, "/api/admin/preorders").hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "STAFF", "ROLE_STAFF")
                .requestMatchers(HttpMethod.GET, "/api/admin/product-questions").hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "STAFF", "ROLE_STAFF")
                .requestMatchers(HttpMethod.PATCH, "/api/admin/product-questions/*/answer").hasAnyAuthority("STAFF", "ROLE_STAFF")

                .requestMatchers(HttpMethod.GET, "/api/users", "/api/users/search")
                    .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "STAFF", "ROLE_STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/users/*/roles")
                    .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")
                .requestMatchers(HttpMethod.PATCH, "/api/users/*/activation")
                    .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")

                .requestMatchers(HttpMethod.POST, "/api/vouchers/validate").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/products/*/questions").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/notifications/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/notifications/**").authenticated()

                .requestMatchers(HttpMethod.GET,
                    "/api/products/**",
                    "/api/categories/**",
                    "/api/brands/**",
                    "/api/attribute-groups/**",
                    "/api/attribute-definitions/**",
                    "/api/category-attributes/**",
                    "/api/vouchers/active",
                    "/api/sale-programs/active",
                    "/api/sale-programs/*"
                ).permitAll()

                .requestMatchers(HttpMethod.GET, "/api/carousel/all", "/api/vouchers", "/api/vouchers/*", "/api/sale-programs")
                    .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")

                .requestMatchers("/api/orders", "/api/orders/**").authenticated()
                .requestMatchers("/api/users", "/api/users/**").authenticated()
                .requestMatchers("/api/cart", "/api/cart/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
