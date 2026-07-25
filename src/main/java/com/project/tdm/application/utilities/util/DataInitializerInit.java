package com.project.tdm.application.utilities.util;

import com.project.tdm.application.entity.PageEntity;
import com.project.tdm.application.entity.RoleEntity;
import com.project.tdm.application.service.PageService;
import com.project.tdm.application.service.RoleService;
import com.project.tdm.application.utilities.constant.BaseConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializerInit implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializerInit.class);

    private final PageService pageService;

    private final RoleService roleService;

    @Autowired
    public DataInitializerInit(PageService pageService, RoleService roleService) {
        this.pageService = pageService;
        this.roleService = roleService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 1. Check whether there is a viewer role in database, create a viewer role if not exists
        RoleEntity viewerRole = roleService.getDefaultRole(BaseConstants.VIEWER_NAME, BaseConstants.VIEWER_DESC);

        // 2. Check whether the viewer role has access to default pages,
        //    Then, check whether default pages access created in database,
        //    Finally, assign the pages to the role if not exists
        if (viewerRole.getAllowedPages().isEmpty()) {
            Set<PageEntity> pagesAccess = pageService.getDefaultRolePages();
            viewerRole.setAllowedPages(new HashSet<>(pagesAccess));
        }

        // 3. Save the viewer role's default pages access to the database
        roleService.updateDefaultRole(viewerRole);
    }
}
