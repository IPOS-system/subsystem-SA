package com.ipos.ipos_sa.filter;

import com.ipos.ipos_sa.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** JWT authentication filter that validates tokens on each request. */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      String authHeader = request.getHeader("Authorization");
      if (authHeader != null) {
        String token = jwtUtil.extractTokenFromHeader(authHeader);
        if (token != null && jwtUtil.validateToken(token)) {
          String username = jwtUtil.getUsernameFromToken(token);
          String role = jwtUtil.getRoleFromToken(token);

          // Create authentication token
          SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  username, null, Collections.singletonList(authority));

          // Set authentication in security context
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
    } catch (Exception e) {
      log.error("JWT authentication error: {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
  }
}
