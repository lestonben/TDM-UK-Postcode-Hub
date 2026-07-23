package com.project.tdm.application.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.tdm.application.dto.RouteDetailsDTO;
import com.project.tdm.application.entity.PostcodeEntity;
import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.service.PostcodeService;
import com.project.tdm.application.utilities.constant.BaseConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostcodeControllerTest {

    @Mock
    private PostcodeService postcodeService;

    @InjectMocks
    private PostcodeController postcodeController;

    // ==========================================
    // 1. getCurrentUser TESTS
    // ==========================================

    @Test
    void shouldReturn200WithUsernameWhenPrincipalIsUserEntity() {
        UserEntity mockUser = new UserEntity();
        mockUser.setUsername("Bbenn");

        ResponseEntity<?> response = postcodeController.getCurrentUser(mockUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals("Bbenn", body.get("username"));
    }

    @Test
    void shouldReturn401UnauthorizedWhenPrincipalIsNull() {
        ResponseEntity<?> response = postcodeController.getCurrentUser(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    // ==========================================
    // 2. suggestPostcodes TESTS
    // ==========================================

    @Test
    void shouldReturnSuggestionsSuccessfully() {
        List<String> mockSuggestions = List.of("SW10 0AD", "SW10 0AB");
        when(postcodeService.getPostcodeSuggestions("SW10", 5)).thenReturn(mockSuggestions);

        ResponseEntity<?> response = postcodeController.suggestPostcodes("SW10", 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(mockSuggestions, body.get("result"));
    }

    // ==========================================
    // 3. searchRoute TESTS
    // ==========================================

    @Test
    void shouldReturnRouteDetailsOnValidSearch() throws JsonProcessingException {
        RouteDetailsDTO mockRoute = new RouteDetailsDTO();
        when(postcodeService.getPostcodesRoute("SW10 0AD", "M1 1AE")).thenReturn(mockRoute);

        ResponseEntity<?> response = postcodeController.searchRoute("SW10 0AD", "M1 1AE");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(mockRoute, body.get("result"));
    }

    @Test
    void shouldReturn400BadRequestWhenRouteSearchThrowsIllegalArgumentException() throws JsonProcessingException {
        when(postcodeService.getPostcodesRoute("INVALID", "M1 1AE"))
                .thenThrow(new IllegalArgumentException("Invalid origin postcode format"));

        ResponseEntity<?> response = postcodeController.searchRoute("INVALID", "M1 1AE");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid origin postcode format", response.getBody());
    }

    @Test
    void shouldReturn500InternalServerErrorWhenRouteSearchCrashes() throws JsonProcessingException {
        when(postcodeService.getPostcodesRoute("SW10 0AD", "M1 1AE"))
                .thenThrow(new RuntimeException("Database timeout"));

        ResponseEntity<?> response = postcodeController.searchRoute("SW10 0AD", "M1 1AE");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(BaseConstants.UNEXPECTED_ERROR_MSG, response.getBody());
    }

    // ==========================================
    // 4. searchPostcode TESTS
    // ==========================================

    @Test
    void shouldReturnPostcodeDetailWhenFound() {
        PostcodeEntity mockPostcode = new PostcodeEntity();
        when(postcodeService.getPostcode("SW10 0AD")).thenReturn(mockPostcode);

        ResponseEntity<?> response = postcodeController.searchPostcode("SW10 0AD");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(mockPostcode, body.get("result"));
    }

    @Test
    void shouldReturn400BadRequestWhenPostcodeSearchThrowsIllegalArgumentException() {
        when(postcodeService.getPostcode("BADCODE"))
                .thenThrow(new IllegalArgumentException("Postcode does not exist"));

        ResponseEntity<?> response = postcodeController.searchPostcode("BADCODE");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Postcode does not exist", response.getBody());
    }

    @Test
    void shouldReturn500InternalServerErrorWhenPostcodeSearchCrashes() {
        when(postcodeService.getPostcode("SW10 0AD"))
                .thenThrow(new RuntimeException("SQL Exception"));

        ResponseEntity<?> response = postcodeController.searchPostcode("SW10 0AD");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(BaseConstants.UNEXPECTED_ERROR_MSG, response.getBody());
    }

    // ==========================================
    // 5. insertOrUpdatePostcode TESTS
    // ==========================================

    @Test
    void shouldInsertOrUpdatePostcodeSuccessfully() {
        Map<String, String> params = Map.of(
                "postcode", "SW10 0AD",
                "latitude", "51.4856",
                "longitude", "-0.1794"
        );
        when(postcodeService.insertOrUpdatePostcode("SW10 0AD", 51.4856, -0.1794))
                .thenReturn("Postcode saved successfully");

        ResponseEntity<?> response = postcodeController.insertOrUpdatePostcode(params);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Postcode saved successfully", response.getBody());
    }

    @Test
    void shouldReturn500InternalServerErrorWhenInsertionCrashes() {
        Map<String, String> params = Map.of(
                "postcode", "SW10 0AD",
                "latitude", "51.4856",
                "longitude", "-0.1794"
        );
        when(postcodeService.insertOrUpdatePostcode("SW10 0AD", 51.4856, -0.1794))
                .thenThrow(new RuntimeException("Write failed"));

        ResponseEntity<?> response = postcodeController.insertOrUpdatePostcode(params);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(BaseConstants.UNEXPECTED_ERROR_MSG, response.getBody());
    }
}