package com.findeks.miniscore.config;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findeks.miniscore.dto.ErrorResponse;
import com.findeks.miniscore.security.JwtAuthFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/*
 * @EnableMethodSecurity: @PreAuthorize / @PostAuthorize gibi METOT seviyesi
 * guvenlik annotation'larini aktif eder. Bu satir olmadan @PreAuthorize
 * SESSIZCE calismaz -> hata vermez, uygulama acilir, ama admin uclari korumasiz kalir.
 * En tehlikeli hata turu budur: gorunurde her sey yolundadir.
 */
@Configuration @EnableWebSecurity @EnableMethodSecurity @RequiredArgsConstructor
public class SecurityConfig {
 
   private final JwtAuthFilter jwtAuthFilter;
   private final UserDetailsService userDetailsService;
   // Spring Boot'un hazir Jackson bean'i. Filtre seviyesindeki hatalari
   // ErrorResponse ile ayni JSON sekline cevirmek icin kullaniyoruz.
   private final ObjectMapper objectMapper;
 
   @Bean
   public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
       return http
          .csrf(AbstractHttpConfigurer::disable)
           .cors(cors -> cors.configurationSource(corsConfigSource()))
           .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
           .authorizeHttpRequests(auth -> auth
              .requestMatchers("/api/auth/**").permitAll()
              .requestMatchers("/api/admin/**").hasRole("ADMIN")
               .anyRequest().authenticated()
           )
           .exceptionHandling(ex -> ex
                   // token yok/gecersiz -> 401
                   .authenticationEntryPoint(restAuthEntryPoint())
                   // token gecerli ama rol yetmiyor (SecurityConfig'teki hasRole reddi) -> 403
                   .accessDeniedHandler(restAccessDeniedHandler()))
           .addFilterBefore(jwtAuthFilter,
                  UsernamePasswordAuthenticationFilter.class)
           .build();
   }
 
   // AuthenticationEntryPoint = "kimligini hic dogrulayamadim" durumunun cevabini uretir.
   // Bu kod filtre zincirinde calisir, DispatcherServlet'e hic girmez ->
   // @RestControllerAdvice DEVREDE DEGILDIR, JSON'i elle yazmak zorundayiz.
   @Bean
   public AuthenticationEntryPoint restAuthEntryPoint() {
       return (request, response, authException) ->
               writeJson(response, HttpStatus.UNAUTHORIZED, "Giriş yapmanız gerekiyor.");
   }

   // AccessDeniedHandler = "kimligin belli ama yetkin yok" (URL seviyesi reddi,
   // yani yukaridaki .hasRole("ADMIN") kurali).
   @Bean
   public AccessDeniedHandler restAccessDeniedHandler() {
       return (request, response, deniedException) ->
               writeJson(response, HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok.");
   }

   // Ortak yardimci: istemci hatanin filtreden mi controller'dan mi geldigini
   // bilmek zorunda kalmasin diye ayni ErrorResponse seklini uretiyoruz.
   private void writeJson(HttpServletResponse response, HttpStatus status, String message)
           throws IOException {
       response.setStatus(status.value());
       response.setContentType(MediaType.APPLICATION_JSON_VALUE);
       response.setCharacterEncoding("UTF-8");   // Turkce karakterler icin sart
       objectMapper.writeValue(response.getWriter(), new ErrorResponse(message));
   }

   @Bean
   public PasswordEncoder passwordEncoder() {
       return new BCryptPasswordEncoder();
   }
 
   @Bean
   public AuthenticationManager authManager(
           AuthenticationConfiguration config) throws Exception {
       return config.getAuthenticationManager();
   }
 
   private CorsConfigurationSource corsConfigSource() {
       CorsConfiguration config = new CorsConfiguration();
      config.setAllowedOrigins(List.of("http://localhost:5173"));
      config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
      config.setAllowedHeaders(List.of("*"));
       UrlBasedCorsConfigurationSource source =
               new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", config);
       return source;
   }
}