/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.config;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kapeta.spring.rest.OpenAPIRedirectController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Default configuration for kapeta
 */
public class KapetaDefaultConfig {

    public static ObjectMapper createDefaultObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
    }


    /**
     * Is needed to support @Value("${some.config.property}")
     */
    @Bean
    @ConditionalOnMissingBean(PropertySourcesPlaceholderConfigurer.class)
    public PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return createDefaultObjectMapper();
    }

    @Bean
    public KapetaController kapetaController() {
        return new KapetaController();
    }

    @Bean
    @ConditionalOnProperty(name = "kapeta.openapi-redirect", havingValue = "true", matchIfMissing = true)
    public OpenAPIRedirectController openAPIRedirectController() {
        return new OpenAPIRedirectController();
    }
}
