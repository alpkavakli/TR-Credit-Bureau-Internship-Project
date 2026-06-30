package com.findeks.miniscore.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component @RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
 
   private final JwtService jwtService;
   private final UserDetailsService userDetailsService;
 
   @Override
   protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                   FilterChain filterChain)
           throws ServletException, IOException, java.io.IOException {
 
       String authHeader = request.getHeader("Authorization");
       if (authHeader == null || !authHeader.startsWith("Bearer ")) {
           filterChain.doFilter(request, response);
           return;
       }
 
       String jwt   = authHeader.substring(7);
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
       filterChain.doFilter(request, response);
   }
}