package com.findeks.miniscore.config;

import java.util.List;

import com.findeks.miniscore.security.JwtAuthFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration @EnableWebSecurity @RequiredArgsConstructor
public class SecurityConfig {
 
   private final JwtAuthFilter jwtAuthFilter;
   private final UserDetailsService userDetailsService;
 
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
           .addFilterBefore(jwtAuthFilter,
                  UsernamePasswordAuthenticationFilter.class)
           .build();
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