package com.project.tdm.security.filters;

import com.project.tdm.application.entity.PageEntity;
import com.project.tdm.application.entity.RoleEntity;
import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.repository.RoleRepo;
import com.project.tdm.application.repository.UserRepo;
import com.project.tdm.application.utilities.constant.BaseConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

@Component
public class PageAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(PageAuthFilter.class);

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private UserRepo userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        logger.info("Incoming requestUri: {}", requestUri);

        if (requestUri.startsWith("/api/")) {
            logger.info("The request is skipping this filter for requestUri: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        logger.info("Verifying page access of the user for the requestUri: {}", requestUri);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {

            Object principal = authentication.getPrincipal();
            String username = ((UserEntity) principal).getUsername();
            logger.info("Authenticated user found for access check: {}", username);

            boolean isAuthorised = userRepo.findByUsernameIgnoreCase(username).get().getUserRoles()
                    .stream()
                    .map(RoleEntity::getAllowedPages)
                    .flatMap(Collection::stream)
                    .map(PageEntity::getUrlPattern)
                    .anyMatch(url -> url.equals(requestUri));

            logger.info("Authorization status: authorized: {}, for user: {}, requestUri: {}", isAuthorised, username, requestUri);

            if (!isAuthorised) {
                if (requestUri.startsWith("/tdm/dashboard/")) {
                    logger.warn("Access denied: User '{}' attempted to access restricted dashboard path '{}'. Forwarding to error page.", username, requestUri);
                    request.getRequestDispatcher(BaseConstants.ERROR_PAGE_URL).forward(request, response);
                    return;
                }
            }
        }
        else {
            logger.debug("No authenticated user found in SecurityContext for protected requestUri: {}", requestUri);
        }

        filterChain.doFilter(request, response);
    }
}
