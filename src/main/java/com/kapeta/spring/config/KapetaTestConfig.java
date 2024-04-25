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
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kapeta.spring.config.pageable.PageableDeserializer;
import com.kapeta.spring.config.pageable.PageableSerializer;
import com.kapeta.spring.config.providers.KapetaConfigurationProvider;
import com.kapeta.spring.config.providers.TestConfigProvider;
import com.kapeta.spring.security.AuthorizationForwarderSupplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static com.kapeta.spring.config.KapetaDefaultConfig.createDefaultObjectMapper;

/**
 * Test configuration for kapeta
 *
 * See {@link com.kapeta.spring.annotation.KapetaTestContext} for how to enable this
 */

public class KapetaTestConfig {

    @Bean
    public KapetaConfigurationProvider kapetaConfigurationProvider(List<TestConfigProvider.TestConfigurationAdjuster> adjusters, ConfigurableEnvironment environment) throws Exception {
        var out = new TestConfigProvider(environment);

        adjusters.forEach(a -> a.adjust(out));

        var propertiesConfigurationSource = new PropertiesConfigurationSource(out);
        out.getEnvironment().getPropertySources().addFirst(propertiesConfigurationSource);

        propertiesConfigurationSource.setProperty(PropertiesConfigurationSource.KAPETA_SYSTEM_TYPE, out.getProviderId());
        propertiesConfigurationSource.setProperty(PropertiesConfigurationSource.KAPETA_SYSTEM_ID, out.getSystemId());
        propertiesConfigurationSource.setProperty(PropertiesConfigurationSource.KAPETA_BLOCK_REF, out.getBlockRef());
        propertiesConfigurationSource.setProperty(PropertiesConfigurationSource.KAPETA_INSTANCE_ID, out.getInstanceId());

        return out;
    }

    /**
     * Is needed to support @Value("${some.config.property}")
     */
    @Bean
    @ConditionalOnMissingBean(PropertySourcesPlaceholderConfigurer.class)
    public PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
