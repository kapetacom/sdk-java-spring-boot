/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapeta.spring.rest.KapetaController;
import com.kapeta.spring.rest.OpenAPIRedirectController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default controllers for kapeta
 */

@Configuration
public class KapetaRestControllerConfig {

    /**
     * A controller that provides a standard endpoints used by kapeta
     */
    @Bean
    @ConditionalOnProperty(prefix = "kapeta.routes", name = "enabled", havingValue = "true", matchIfMissing = true)
    public KapetaController kapetaController(ObjectMapper objectMapper) {
        return new KapetaController(objectMapper);
    }

    /**
     * A controller that redirects to the OpenAPI documentation. Disable this if you need to use the root path (GET /)
     * for something else.
     */
    @Bean
    @ConditionalOnProperty(prefix = "kapeta.docs", name = "redirect", havingValue = "true", matchIfMissing = true)
    public OpenAPIRedirectController openAPIRedirectController() {
        return new OpenAPIRedirectController();
    }
}
