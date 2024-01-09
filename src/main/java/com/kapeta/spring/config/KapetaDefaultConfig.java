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
import com.kapeta.spring.security.AuthorizationForwarderSupplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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

    /**
     * A default object mapper that can be used by other modules.
     * <p>
     * Will use sensible defaults for JSON serialization/deserialization that makes it easy to work with
     * <p>
     * See {@link #createDefaultObjectMapper()} for details
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return createDefaultObjectMapper();
    }


    /**
     * Provides a way to forward authorization headers to another service
     * This is used by the REST Client SDK to automatically forward JWT to other internal services
     */
    @Bean
    @ConditionalOnMissingBean(AuthorizationForwarderSupplier.class)
    public AuthorizationForwarderSupplier authorizationForwarderSupplier() {
        return () -> null;
    }

}
