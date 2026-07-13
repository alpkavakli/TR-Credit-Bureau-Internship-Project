package com.findeks.miniscore.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component @RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
 
   private final JwtService jwtService;
   private final UserDetailsService userDetailsService;

   // /api/auth/** token verir, token dogrulamaz; header'da ne gelirse gelsin bakma
   @Override
   protected boolean shouldNotFilter(HttpServletRequest request) {
       return request.getServletPath().startsWith("/api/auth/");
   }

   @Override
   protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                   FilterChain filterChain)
           throws ServletException, IOException {
 
       String authHeader = request.getHeader("Authorization");
       if (authHeader == null || !authHeader.startsWith("Bearer ")) {
           filterChain.doFilter(request, response);
           return;
       }
 
       String jwt = authHeader.substring(7);

       try {
           String email = jwtService.extractUsername(jwt);

           if (email != null && SecurityContextHolder.getContext()
                   .getAuthentication() == null) {
               UserDetails user = userDetailsService.loadUserByUsername(email);
               if (jwtService.isTokenValid(jwt, user)) {
                   var auth = new UsernamePasswordAuthenticationToken(
                           user, null, user.getAuthorities());
                   auth.setDetails(new WebAuthenticationDetailsSource()
                          .buildDetails(request));
                  SecurityContextHolder.getContext().setAuthentication(auth);
               }
           }
       } catch (JwtException e) {
           response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
           response.setContentType("application/json;charset=UTF-8");
           response.getWriter().write("{\"message\":\"Geçersiz token.\"}");
           return;
       }

       filterChain.doFilter(request, response);
   }
}