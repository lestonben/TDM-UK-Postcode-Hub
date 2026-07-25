package com.project.tdm.application.service.impl;

import com.project.tdm.application.entity.PageEntity;
import com.project.tdm.application.repository.PageRepo;
import com.project.tdm.application.service.PageService;
import com.project.tdm.application.utilities.constant.BaseConstants;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PageServiceImpl implements PageService {

    private static final Logger logger = LoggerFactory.getLogger(PageServiceImpl.class);

    @Autowired
    private PageRepo pageRepo;

    private HashMap<String, String> getDefaultPagesMap() {
        return new HashMap<>(){{
            put(BaseConstants.DASHBOARD_MAIN_NAME, BaseConstants.DASHBOARD_MAIN_URL);
            put(BaseConstants.DASHBOARD_UPDATE_NAME, BaseConstants.DASHBOARD_UPDATE_URL);
            put(BaseConstants.DASHBOARD_ROLE_MANAGEMENT_NAME, BaseConstants.DASHBOARD_ROLE_MANAGEMENT_URL);
        }};
    }

    @Override
    @Transactional
    public Set<PageEntity> getDefaultRolePages() {
        // 1. Retrieve a list of declared default pages
        Map<String, String> defaultPagesMap = getDefaultPagesMap();
        logger.info("getDefaultRolePages(): Initialize the map of default pages: {}", defaultPagesMap.keySet());

        // 2. Check whether the declared pages exist from database
        Set<PageEntity> existingDefaultPages = pageRepo.findByUrlPatternIn(new HashSet<>(getDefaultPagesMap().values()));
        logger.info("getDefaultRolePages(): Check if there are default pages declared in the database: {}", existingDefaultPages.size());

        // 3. Early exit if the DB default pages >= initial default pages
        if (existingDefaultPages.size() >= defaultPagesMap.size()) {
            logger.info("getDefaultRolePages(): Skipping to generate, database records found.");
            return existingDefaultPages;
        }

        // 4. Iterate the declared list of default pages, compare pages from database to see which are missing ones, then add to a temp list if not exists
        Set<String> existingDefaultPageUri = existingDefaultPages.stream().map(PageEntity::getUrlPattern).collect(Collectors.toSet());
        List<PageEntity> nonExistDefaultPages = new ArrayList<>();
        logger.info("getDefaultRolePages(): Compare and search for missing default pages that require to be added in database.");
        for (Map.Entry<String, String> defaultPage : defaultPagesMap.entrySet()) {
            String pageUrl = defaultPage.getValue();

            if (existingDefaultPageUri.contains(pageUrl)) {
                logger.info("getDefaultRolePages(): Skipping to include this page: {}", pageUrl);
                continue;
            }

            PageEntity newDefaultPage = new PageEntity(defaultPage.getKey(), pageUrl);
            nonExistDefaultPages.add(newDefaultPage);
            logger.info("getDefaultRolePages(): The database is missing this page: {}", pageUrl);
        }

        // 4. Save missing pages to the database if any were found
        List<PageEntity> addedDefaultPages = pageRepo.saveAll(nonExistDefaultPages);
        existingDefaultPages.addAll(addedDefaultPages);
        logger.info("getDefaultRolePages(): The updated list of default pages declared in the database: {}", existingDefaultPages.size());

        return existingDefaultPages;
    }

    @Override
    public Set<PageEntity> getPagesByUrlPatterns(List<String> urlPatterns) {
        Set<PageEntity> pageList = pageRepo.findByUrlPatternIn(new HashSet<>(urlPatterns));
        logger.info("getPagesByUrlPatterns(): pageList result: {}", (pageList == null ? 0 : pageList.size()));

        return pageList;
    }
}
