package com.project.tdm.security.config;

import com.project.tdm.application.utilities.constant.BaseConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController(BaseConstants.HOME_URL).setViewName("forward:/templates/index.html");
        registry.addRedirectViewController(BaseConstants.DEFAULT_URL, BaseConstants.HOME_URL);

        registry.addViewController(BaseConstants.DASHBOARD_MAIN_URL).setViewName("forward:/templates/dashboard-main.html");

        registry.addViewController(BaseConstants.DASHBOARD_UPDATE_URL).setViewName("forward:/templates/dashboard-update.html");

        registry.addViewController(BaseConstants.DASHBOARD_ROLE_MANAGEMENT_URL).setViewName("forward:/templates/dashboard-role-management.html");

        registry.addViewController(BaseConstants.ERROR_PAGE_URL).setViewName("forward:/templates/error-page.html");
    }

}
