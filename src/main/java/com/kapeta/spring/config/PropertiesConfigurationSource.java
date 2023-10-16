package com.kapeta.spring.config;

import com.kapeta.spring.config.providers.KapetaConfigurationProvider;
import org.springframework.core.env.PropertySource;

import java.util.Properties;

import static com.kapeta.spring.config.ConfigUtils.applyFlattenedObjectToProperties;


public class PropertiesConfigurationSource extends PropertySource<Object> {
    private static final String SERVER_PORT = "server.port";

    private static final String SERVER_HOST = "server.host";

    private final Properties properties;

    public PropertiesConfigurationSource(KapetaConfigurationProvider configurationProvider) throws Exception {
        super(configurationProvider.getProviderId());

        properties = new Properties();

        var instanceConfig = configurationProvider.getInstanceConfig();

        applyFlattenedObjectToProperties(configurationProvider.getEnvironment(), instanceConfig, properties);

        properties.put(SERVER_PORT, configurationProvider.getServerPort());
        properties.put(SERVER_HOST, configurationProvider.getServerHost());
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

}
