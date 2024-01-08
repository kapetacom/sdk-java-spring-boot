/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */

package com.kapeta.spring.config;

import com.kapeta.spring.config.providers.KapetaConfigurationProvider;
import org.springframework.core.env.PropertySource;

import java.util.Properties;

import static com.kapeta.spring.config.ConfigUtils.applyFlattenedObjectToProperties;


public class PropertiesConfigurationSource extends PropertySource<Object> {

    public static final String KAPETA_BLOCK_REF = "kapeta.block.ref";
    public static final String KAPETA_INSTANCE_ID = "kapeta.instance.id";
    public static final String KAPETA_SYSTEM_ID = "kapeta.system.id";
    public static final String KAPETA_SYSTEM_TYPE = "kapeta.system.type";

    private static final String SERVER_PORT = "server.port";
    private static final String SERVER_HOST = "server.host";

    private final Properties properties;

    public PropertiesConfigurationSource(KapetaConfigurationProvider configurationProvider) throws Exception {
        super(configurationProvider.getProviderId());

        properties = new Properties();

        var instanceConfig = configurationProvider.getInstanceConfig();

        applyFlattenedObjectToProperties(configurationProvider.getEnvironment(), instanceConfig, properties);

        setProperty(SERVER_PORT, configurationProvider.getServerPort());
        setProperty(SERVER_HOST, configurationProvider.getServerHost());
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    void setProperty(String name, Object value) {
        properties.put(name, value);
    }

}
