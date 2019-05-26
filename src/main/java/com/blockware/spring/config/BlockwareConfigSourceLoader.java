package com.blockware.spring.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import java.net.MalformedURLException;

public class BlockwareConfigSourceLoader implements ApplicationListener<ApplicationPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(BlockwareConfigSource.class);

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        final ConfigurableApplicationContext applicationContext = event.getApplicationContext();

        final ConfigurableEnvironment environment = applicationContext.getEnvironment();

        final String serviceName = environment.getProperty("spring.application.name");
        try {
            BlockwareConfigSource configServer = new BlockwareConfigSource(serviceName, environment);

            configServer.load();
            MutablePropertySources propertySources = environment.getPropertySources();
            propertySources.addFirst(configServer);
            log.info("Blockware configuration server initialised");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to initialise blockware config service", e);
        }
    }
}
