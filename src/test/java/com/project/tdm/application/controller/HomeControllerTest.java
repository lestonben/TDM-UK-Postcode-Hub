package com.project.tdm.application.controller;

import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.service.UserService;
import com.project.tdm.application.utilities.constant.BaseConstants;
import com.project.tdm.security.util.CookieUtil;
import com.project.tdm.security.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private HomeController homeController;

    // ==========================================
    // redirectLogin TESTS
    // ==========================================

    @Test
    void shouldRedirectToDashboardWhenValidTokenExists() {
        String mockToken = "valid.jwt.token";
        when(cookieUtil.searchCookieValue(request, BaseConstants.JWT_TOKEN)).thenReturn(mockToken);
        when(jwtUtil.isTokenExpired(mockToken)).thenReturn(false);

        ResponseEntity<?> result = (ResponseEntity<?>) homeController.redirectLogin(request, response);

        assertEquals(HttpStatus.FOUND, result.getStatusCode());
        assertEquals("/tdm/dashboard/main", result.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    void shouldRedirectToHomeWhenTokenIsMissing() {
        when(cookieUtil.searchCookieValue(request, BaseConstants.JWT_TOKEN)).thenReturn(null);

        ResponseEntity<?> result = (ResponseEntity<?>) homeController.redirectLogin(request, response);

        assertEquals(HttpStatus.FOUND, result.getStatusCode());
        assertEquals("/tdm/home", result.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    // ==========================================
    // login TESTS
    // ==========================================

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        Map<String, String> loginRequest = Map.of(
                "usernameEmail", "testuser",
                "password", "password123"
        );
        UserEntity mockUser = new UserEntity();
        String mockToken = "generated.jwt.token";
        Cookie mockCookie = new Cookie(BaseConstants.JWT_TOKEN, mockToken);

        when(userService.loginUser(any(UserEntity.class))).thenReturn(mockUser);
        when(jwtUtil.generateToken(mockUser)).thenReturn(mockToken);
        when(cookieUtil.setCookieAlive(BaseConstants.JWT_TOKEN, mockToken)).thenReturn(mockCookie);

        // Call the method
        ResponseEntity<?> result = homeController.login(loginRequest, response);

        // 1. Verify status
        assertEquals(HttpStatus.OK, result.getStatusCode());

        // 2. Access the body directly from ResponseEntity
        Object responseBody = result.getBody();
        assertNotNull(responseBody);

        // 3. Compare the body (if you expect an empty string or specific message)
        assertEquals("", responseBody);
    }

    @Test
    void shouldReturn400BadRequestWhenLoginCredentialsInvalid() throws Exception {
        Map<String, String> loginRequest = Map.of(
                "usernameEmail", "wronguser",
                "password", "wrongpass"
        );
        when(userService.loginUser(any(UserEntity.class)))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        ResponseEntity<?> result = homeController.login(loginRequest, response);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("Invalid credentials", result.getBody());
    }

    // ==========================================
    // logout TESTS
    // ==========================================

    @Test
    void shouldLogoutSuccessfullyAndExpireCookie() {
        Cookie expiredCookie = new Cookie(BaseConstants.JWT_TOKEN, "");
        when(cookieUtil.setCookieExpired(BaseConstants.JWT_TOKEN)).thenReturn(expiredCookie);

        ResponseEntity<?> result = homeController.logout(response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(response).addCookie(expiredCookie);
    }
}