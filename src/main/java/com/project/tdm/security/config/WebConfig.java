package com.project.tdm.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/tdm/home").setViewName("forward:/index.html");
        registry.addRedirectViewController("/", "/tdm/home");

        registry.addViewController("/tdm/dashboard/main").setViewName("forward:/assets/dashboard/main.html");

        registry.addViewController("/tdm/dashboard/update").setViewName("forward:/assets/dashboard/update.html");
    }

}
