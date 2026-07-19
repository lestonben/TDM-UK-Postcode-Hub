package com.project.tdm.security.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CookieUtilTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CookieUtil cookieUtil;

    // ==========================================
    // 1. setCookieAlive TESTS
    // ==========================================

    @Test
    void shouldCreateAnActiveSecureCookie() {
        // Act
        Cookie cookie = cookieUtil.setCookieAlive("jwtToken", "mock.jwt.value");

        // Assert
        assertNotNull(cookie);
        assertEquals("jwtToken", cookie.getName());
        assertEquals("mock.jwt.value", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals("/", cookie.getPath());
        assertEquals(86400, cookie.getMaxAge()); // 24 * 60 * 60
    }

    // ==========================================
    // 2. searchCookieValue TESTS
    // ==========================================

    @Test
    void shouldReturnCookieValueWhenCookieExists() {
        // Arrange
        Cookie[] mockCookies = {
                new Cookie("session", "xyz"),
                new Cookie("jwtToken", "targetValue"),
                new Cookie("theme", "dark")
        };
        when(request.getCookies()).thenReturn(mockCookies);

        // Act
        String result = cookieUtil.searchCookieValue(request, "jwtToken");

        // Assert
        assertEquals("targetValue", result);
    }

    @Test
    void shouldReturnEmptyStringWhenCookiesArrayIsNull() {
        // Arrange
        when(request.getCookies()).thenReturn(null);

        // Act
        String result = cookieUtil.searchCookieValue(request, "jwtToken");

        // Assert
        assertEquals("", result);
    }

    @Test
    void shouldReturnEmptyStringWhenTargetCookieIsMissing() {
        // Arrange
        Cookie[] mockCookies = {
                new Cookie("session", "xyz")
        };
        when(request.getCookies()).thenReturn(mockCookies);

        // Act
        String result = cookieUtil.searchCookieValue(request, "jwtToken");

        // Assert
        assertEquals("", result);
    }

    // ==========================================
    // 3. setCookieExpired TESTS
    // ==========================================

    @Test
    void shouldCreateAnExpiredCookie() {
        // Act
        Cookie cookie = cookieUtil.setCookieExpired("jwtToken");

        // Assert
        assertNotNull(cookie);
        assertEquals("jwtToken", cookie.getName());
        assertNull(cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals("/", cookie.getPath());
        assertEquals(0, cookie.getMaxAge()); // Expired instantly
    }
}