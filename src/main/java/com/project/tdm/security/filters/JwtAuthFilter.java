package com.project.tdm.security.filters;

import com.project.tdm.application.entity.RoleEntity;
import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.service.UserService;
import com.project.tdm.application.utilities.constant.BaseConstants;
import com.project.tdm.security.util.CookieUtil;
import com.project.tdm.security.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

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
        String requestUri = request.getRequestURI();
        String tokenStr = resolveToken(request);
        logger.info("Incoming requestUri: {}", requestUri);

        if (shouldNotFilter(request) || (tokenStr == null || tokenStr.trim().isEmpty())) {
            logger.info("The request is skipping this filter or has no JWT token found in cookies for requestUri: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        logger.info("Verifying JWT token of the user for the requestUri: {}", requestUri);
        try {
            String tokenUsername = jwtUtil.extractClaim(tokenStr, Claims::getSubject);

            if (tokenUsername != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserEntity userByToken = userService.getUserByUsername(tokenUsername);

                if (userByToken != null && jwtUtil.validateToken(tokenStr, userByToken)) {
                    List<GrantedAuthority> userAuthRolesList =
                            Optional.ofNullable(userByToken.getUserRoles())
                                    .stream()
                                    .flatMap(Collection::stream)
                                    .map(RoleEntity::getName)
                                    .map(roleName -> (GrantedAuthority) new SimpleGrantedAuthority(roleName))
                                    .collect(Collectors.toCollection(ArrayList::new));

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userByToken, null, userAuthRolesList);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("JWT validation success. The request has been verified for the user: {}", tokenUsername);
                }
                else {
                    logger.warn("JWT validation failed. The user does not exist in database for username extracted from token: {}", tokenUsername);
                }
            }
        }
        catch (Exception ex) {
            logger.error("Cannot set user authentication: Failed to parse or validate JWT token: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
