package com.project.tdm.security.filters;

import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.service.UserService;
import com.project.tdm.application.util.BaseConstants;
import com.project.tdm.security.util.CookieUtil;
import com.project.tdm.security.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private CookieUtil cookieUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    private String resolveToken(HttpServletRequest request) {
        return cookieUtil.searchCookieValue(request, BaseConstants.JWT_TOKEN);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        return "/".equals(path)
                || "/tdm/home".equals(path)
                || "/api/login".equals(path)
                || "/api/register".equals(path)
                || path.startsWith("/assets/")
                || path.endsWith(".html")
                || path.endsWith(".js")
                || path.endsWith(".css")
                || path.endsWith(".ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tokenStr = resolveToken(request);

        if (shouldNotFilter(request) || (tokenStr == null || tokenStr.trim().isEmpty())) {
            filterChain.doFilter(request, response);
            return;
        }
        System.out.println("path "+request.getRequestURI());

        String tokenUsername = jwtUtil.extractClaim(tokenStr, Claims::getSubject);
        if (tokenUsername != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserEntity userByToken = userService.getUserByUsername(tokenUsername);

            if (userByToken != null && jwtUtil.validateToken(tokenStr, userByToken)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userByToken, null, List.of());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
