package com.project.tdm.application.service;

import com.project.tdm.application.entity.PageEntity;
import com.project.tdm.application.repository.PageRepo;
import com.project.tdm.application.service.impl.PageServiceImpl;
import com.project.tdm.application.utilities.constant.BaseConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PageServiceTest {

    @Mock
    private PageRepo pageRepo;

    @InjectMocks
    private PageServiceImpl pageService;

    // ==========================================
    // 1. getDefaultRolePages TESTS
    // ==========================================

    @Test
    void shouldReturnExistingDefaultPagesWhenAllPagesExistInDb() {
        // Arrange
        Set<PageEntity> existingPages = Set.of(
                new PageEntity(BaseConstants.DASHBOARD_MAIN_NAME, BaseConstants.DASHBOARD_MAIN_URL),
                new PageEntity(BaseConstants.DASHBOARD_UPDATE_NAME, BaseConstants.DASHBOARD_UPDATE_URL),
                new PageEntity(BaseConstants.DASHBOARD_ROLE_MANAGEMENT_NAME, BaseConstants.DASHBOARD_ROLE_MANAGEMENT_URL)
        );

        when(pageRepo.findByUrlPatternIn(anySet())).thenReturn(existingPages);

        // Act
        Set<PageEntity> result = pageService.getDefaultRolePages();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(pageRepo, never()).saveAll(any());
    }

    @Test
    void shouldCreateAndSaveMissingDefaultPagesWhenDbIsIncomplete() {
        // Arrange
        PageEntity partialPage = new PageEntity(BaseConstants.DASHBOARD_MAIN_NAME, BaseConstants.DASHBOARD_MAIN_URL);
        Set<PageEntity> partialPagesFromDb = new HashSet<>();
        partialPagesFromDb.add(partialPage);

        when(pageRepo.findByUrlPatternIn(anySet())).thenReturn(partialPagesFromDb);

        // Mock the saveAll behavior to return the newly added pages
        when(pageRepo.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Set<PageEntity> result = pageService.getDefaultRolePages();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(pageRepo, times(1)).saveAll(anyList());
    }

    // ==========================================
    // 2. getPagesByUrlPatterns TESTS
    // ==========================================

    @Test
    void shouldReturnPagesWhenMatchingUrlPatternsExist() {
        // Arrange
        List<String> patterns = List.of(BaseConstants.DASHBOARD_MAIN_URL);
        Set<PageEntity> expectedPages = Set.of(
                new PageEntity(BaseConstants.DASHBOARD_MAIN_NAME, BaseConstants.DASHBOARD_MAIN_URL)
        );

        when(pageRepo.findByUrlPatternIn(anySet())).thenReturn(expectedPages);

        // Act
        Set<PageEntity> result = pageService.getPagesByUrlPatterns(patterns);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(pageRepo, times(1)).findByUrlPatternIn(anySet());
    }

    @Test
    void shouldReturnEmptySetWhenNoMatchingUrlPatternsExist() {
        // Arrange
        List<String> patterns = List.of("/non/existent/url");
        when(pageRepo.findByUrlPatternIn(anySet())).thenReturn(Set.of());

        // Act
        Set<PageEntity> result = pageService.getPagesByUrlPatterns(patterns);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(pageRepo, times(1)).findByUrlPatternIn(anySet());
    }
}