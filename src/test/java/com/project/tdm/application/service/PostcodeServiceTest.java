package com.project.tdm.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.tdm.application.dto.RouteDetailsDTO;
import com.project.tdm.application.entity.PostcodeEntity;
import com.project.tdm.application.repository.PostcodeRedisDao;
import com.project.tdm.application.repository.PostcodeRepo;
import com.project.tdm.application.service.impl.PostcodeServiceImpl;
import com.project.tdm.application.util.BaseConstants;
import com.project.tdm.security.util.GeoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostcodeServiceTest {

    @Mock
    private PostcodeRedisDao postcodeRedisDao;

    @Mock
    private PostcodeRepo postcodeRepo;

    @InjectMocks
    private PostcodeServiceImpl postcodeService;

    @BeforeEach
    void setUp() {
        postcodeService.setPostcodeRepo(postcodeRepo);
    }

    // ==========================================
    // 1. getPostcode TESTS
    // ==========================================

    @Test
    void shouldReturnPostcodeEntityWhenFoundInDb() {
        PostcodeEntity expected = new PostcodeEntity();
        expected.setPostcode("SW10 0AD");
        when(postcodeRepo.findPostcode("SW10 0AD")).thenReturn(Optional.of(expected));

        PostcodeEntity result = postcodeService.getPostcode("  sw10 0ad  ");

        assertNotNull(result);
        assertEquals("SW10 0AD", result.getPostcode());
    }

    @Test
    void shouldThrowExceptionWhenPostcodeNotFoundInDb() {
        when(postcodeRepo.findPostcode("UNKNOWN")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                postcodeService.getPostcode("unknown")
        );
        assertEquals("UNKNOWN is not available in the database.", ex.getMessage());
    }

    // ==========================================
    // 2. getPostcodeSuggestions TESTS
    // ==========================================

    @Test
    void shouldReturnEmptyListWhenQueryIsEmptyOrNull() {
        assertTrue(postcodeService.getPostcodeSuggestions(null, 5).isEmpty());
        assertTrue(postcodeService.getPostcodeSuggestions("   ", 5).isEmpty());
        verifyNoInteractions(postcodeRedisDao, postcodeRepo);
    }

    @Test
    void shouldReturnSuggestionsFromCacheWhenHit() {
        List<String> cached = List.of("SW10 0AD", "SW10 0AB");
        when(postcodeRedisDao.getPostcodeFromCache(BaseConstants.REDIS_KEY_PC_AUTOCOMPLETE, "SW10", 5))
                .thenReturn(cached);

        List<String> result = postcodeService.getPostcodeSuggestions("sw10", 5);

        assertEquals(cached, result);
        verifyNoInteractions(postcodeRepo);
    }

    @Test
    void shouldFallbackToDbAndCacheResultsWhenCacheMisses() {
        List<String> dbResults = List.of("SW10 0AD");
        when(postcodeRedisDao.getPostcodeFromCache(BaseConstants.REDIS_KEY_PC_AUTOCOMPLETE, "SW10", 5))
                .thenReturn(List.of());
        when(postcodeRepo.findPostcodeSuggestions("SW10", PageRequest.of(0, 5)))
                .thenReturn(dbResults);

        List<String> result = postcodeService.getPostcodeSuggestions("sw10", 5);

        assertEquals(dbResults, result);
        verify(postcodeRedisDao).addPostcodeToCache(BaseConstants.REDIS_KEY_PC_AUTOCOMPLETE, "SW10 0AD");
    }

    // ==========================================
    // 3. getPostcodesRoute TESTS
    // ==========================================

    @Test
    void shouldReturnRouteFromCacheWhenHit() throws JsonProcessingException {
        RouteDetailsDTO cachedRoute = new RouteDetailsDTO();
        // SW100AD vs M11AE: alphabetically M11AE is smaller, so key is M11AE:SW100AD
        when(postcodeRedisDao.getRouteFromCache("M11AE:SW100AD")).thenReturn(cachedRoute);

        RouteDetailsDTO result = postcodeService.getPostcodesRoute("SW10 0AD", "M1 1AE");

        assertNotNull(result);
        assertEquals(cachedRoute, result);
        verifyNoInteractions(postcodeRepo);
    }

    @Test
    void shouldCalculateRouteFromDbAndPopulateCacheWhenCacheMisses() throws JsonProcessingException {
        PostcodeEntity fromNode = new PostcodeEntity();
        fromNode.setPostcode("M1 1AE");
        fromNode.setLatitude(53.4808);
        fromNode.setLongitude(-2.2426);

        PostcodeEntity toNode = new PostcodeEntity();
        toNode.setPostcode("SW10 0AD");
        toNode.setLatitude(51.4856);
        toNode.setLongitude(-0.1794);

        when(postcodeRedisDao.getRouteFromCache("M11AE:SW100AD")).thenReturn(null);
        // getPostcodes passes (first, second) which is (M1 1AE, SW10 0AD) alphabetically
        when(postcodeRepo.findPostcodes("M1 1AE", "SW10 0AD")).thenReturn(List.of(fromNode, toNode));

        try (MockedStatic<GeoUtil> mockedGeo = mockStatic(GeoUtil.class)) {
            mockedGeo.when(() -> GeoUtil.calculateDistance(53.4808, -2.2426, 51.4856, -0.1794))
                    .thenReturn(263.5);

            RouteDetailsDTO result = postcodeService.getPostcodesRoute("M1 1AE", "SW10 0AD");

            assertNotNull(result);
            assertEquals(263.5, result.getDistance());
            verify(postcodeRedisDao).addRouteToCache(eq("M11AE:SW100AD"), any(RouteDetailsDTO.class));
        }
    }

    @Test
    void shouldThrowExceptionWhenRoutingFindsMissingPostcodes() {
        when(postcodeRepo.findPostcodes("M1 1AE", "SW10 0AD")).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                postcodeService.getPostcodesRoute("M1 1AE", "SW10 0AD")
        );
        assertTrue(ex.getMessage().contains("Both postcodes"));
    }

    // ==========================================
    // 4. insertOrUpdatePostcode TESTS
    // ==========================================

    @Test
    void shouldCreateNewPostcodeMappingAndAddToAutocompleteCache() {
        when(postcodeRepo.findPostcode("SW10 0AD")).thenReturn(Optional.empty());

        String status = postcodeService.insertOrUpdatePostcode("sw10 0ad", 51.4856, -0.1794);

        assertEquals("New postcode mapping has been created successfully.", status);
        verify(postcodeRepo).save(any(PostcodeEntity.class));
        verify(postcodeRedisDao).addPostcodeToCache(BaseConstants.REDIS_KEY_PC_AUTOCOMPLETE, "SW10 0AD");
        verify(postcodeRedisDao, never()).evictRouteCache(anyString());
    }

    @Test
    void shouldUpdateExistingPostcodeMappingAndEvictRouteCache() {
        PostcodeEntity existing = new PostcodeEntity();
        existing.setPostcode("SW10 0AD");
        when(postcodeRepo.findPostcode("SW10 0AD")).thenReturn(Optional.of(existing));

        String status = postcodeService.insertOrUpdatePostcode("sw10 0ad", 51.5555, -0.1111);

        assertEquals("Postcode mapping has been updated successfully.", status);
        verify(postcodeRepo).save(existing);
        verify(postcodeRedisDao).evictRouteCache("SW10 0AD");
        verify(postcodeRedisDao, never()).addPostcodeToCache(anyString(), anyString());
    }

    @Test
    void shouldNotCrashWhenRedisFailsDuringInsertion() {
        when(postcodeRepo.findPostcode("SW10 0AD")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Redis down"))
                .when(postcodeRedisDao).addPostcodeToCache(anyString(), anyString());

        assertDoesNotThrow(() ->
                postcodeService.insertOrUpdatePostcode("SW10 0AD", 51.4856, -0.1794)
        );
        verify(postcodeRepo, times(1)).save(any(PostcodeEntity.class));
    }
}