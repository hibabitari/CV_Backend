package com.ahmed.pfa.cvplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Configuration pour la pagination
 * Limite le size maximum pour éviter OutOfMemoryError
 */
@Configuration
@EnableSpringDataWebSupport
public class PageableConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setMaxPageSize(100);  // Maximum 100 éléments par page
        resolver.setFallbackPageable(org.springframework.data.domain.PageRequest.of(0, 20));
        resolvers.add(resolver);
    }
}