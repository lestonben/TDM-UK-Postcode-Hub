package com.project.tdm.security.config;

import com.project.tdm.security.filters.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        // Create an MVC matcher factory that uses the standard AntPathMatcher
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Add these to permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/"),
                                mvcMatcherBuilder.pattern("/tdm/home"),
                                mvcMatcherBuilder.pattern("/api/**"),
                                mvcMatcherBuilder.pattern("/assets/**"),
                                mvcMatcherBuilder.pattern("/util/**"),
                                mvcMatcherBuilder.pattern("/*.html"),
                                mvcMatcherBuilder.pattern("/*.css"),
                                mvcMatcherBuilder.pattern("/*.js")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/dashboard/**")).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
