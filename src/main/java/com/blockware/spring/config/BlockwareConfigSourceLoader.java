package com.blockware.spring.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

public class BlockwareConfigSourceLoader implements ApplicationListener<ApplicationPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(BlockwareConfigSource.class);

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        final ConfigurableApplicationContext applicationContext = event.getApplicationContext();

        final ConfigurableEnvironment environment = applicationContext.getEnvironment();

        final String serviceName = getSystemConfiguration(environment,
                "BLOCKWARE_APPLICATION_NAME",
                "spring.application.name",
                "unknown-application");

        final String environmentType = getSystemConfiguration(environment,
                "BLOCKWARE_SYSTEM_TYPE",
                "blockware.environment.type",
                "development").toLowerCase();


        BlockwareConfigSource configSource = null;
        switch (environmentType) {
            case "staging":
            case "sandbox":

            case "production":
            case "prod":
                throw new RuntimeException("Unimplemented environment support: " + environmentType);

            case "development":
            case "dev":
            case "local":
                configSource = new BlockwareConfigSourceLocal(serviceName, environment);
                break;

            default:
                throw new RuntimeException("Unknown environment: " + environmentType);

        }

        try {

            configSource.load();
            MutablePropertySources propertySources = environment.getPropertySources();
            propertySources.addFirst(configSource);

            applicationContext.getBeanFactory().registerResolvableDependency(BlockwareConfigSource.class, configSource);

            log.info("Blockware configuration source initialised: {} for environment: {}", configSource.getSourceId(), environmentType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise blockware config source for environment: " + environmentType, e);
        }
    }

    static String getSystemConfiguration(final Environment environment, String environmentKey, String configKey, String defaultValue) {
        return getSystemConfiguration(environment, environmentKey, environmentKey, configKey, defaultValue);
    }

    private static String getSystemConfiguration(final Environment environment, String propertyKey, String environmentKey, String configKey, String defaultValue) {
        String value = System.getProperty(propertyKey);
        if (!StringUtils.isEmpty(value)) {
            return value;
        }

        value = System.getenv(environmentKey);

        if (!StringUtils.isEmpty(value)) {
            return value;
        }

        return environment.getProperty(configKey, defaultValue);
    }
}
