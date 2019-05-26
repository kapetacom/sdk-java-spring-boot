package com.blockware.spring.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class BlockwareConfigSource extends PropertySource<URL> {

    private static final Logger log = LoggerFactory.getLogger(BlockwareConfigSource.class);

    public static final String BLOCKWARE_CONFIG_SERVICE = "BLOCKWARE_CONFIG_SERVICE";

    public static final String BLOCKWARE_CONFIG_SERVICE_DEFAULT = "http://localhost:30033/";

    private final String serviceName;

    private Properties properties = new Properties();
    
    private final Environment environment;

    private boolean initialized;
    
    public BlockwareConfigSource(String serviceName, Environment environment) throws MalformedURLException {
        super("BLOCKWARE_SERVICE");
        this.environment = environment;
        this.serviceName = serviceName;
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    private void applyConfigurationFromConfigService(Properties properties) throws MalformedURLException {
        File file;
        URL configUrl = getConfigServiceUrl();
        
        readConfigurationFromUrl(configUrl, properties);

        if (initialized) {
            log.debug("Read config from: {}", configUrl);
        } else {
            log.info("Read config from: {}", configUrl);
        }

        int envVars = applyOverridesFromEnvironment();
        log.trace("Read {} 'BLOCKWARE_<section>_<key>=<value>' environment variables (overwrites values from files)", envVars);
    }

    private void readConfigurationFromUrl(URL configUrl, Properties properties) {
        switch (serviceName) {
            case "todo":
                properties.put("server.port", "6001");
                break;
            case "users":
                properties.put("server.port", "6002");
                break;
        }
    }

    private String getPropertyValue(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (!StringUtils.isEmpty(value)) {
            return value;
        }

        value = System.getenv(key);

        if (!StringUtils.isEmpty(value)) {
            return value;
        }

        return environment.getProperty(key, defaultValue);
    }

    private URL getConfigServiceUrl() throws MalformedURLException {

        String hostname = getPropertyValue(BLOCKWARE_CONFIG_SERVICE, BLOCKWARE_CONFIG_SERVICE_DEFAULT);

        if (!hostname.endsWith("/")) {
            hostname += "/";
        }

        return new URL(hostname + serviceName);
    }

    private int applyOverridesFromEnvironment() {
        return applyOverridesFromEnvironment(properties);
    }

    private int applyOverridesFromEnvironment(Properties properties) {
        int envVarsRead = 0;

        Map<String, String> env = System.getenv();
        Set<String> envKeys = env.keySet();
        if (envKeys.size() > 0) {
            for (String envKey : envKeys) {
                if (envKey.startsWith("BLOCKWARE_")) {
                    // Format: BLOCKWARE_<section>_<key> = <value>

                    String envKeyWithoutPrefix = envKey.substring(5).toLowerCase();
                    if (envKeyWithoutPrefix.indexOf("_") == -1) {
                        continue;
                    }
                    String section = envKeyWithoutPrefix.substring(0, envKeyWithoutPrefix.indexOf("_"));
                    String key = envKeyWithoutPrefix.substring(envKeyWithoutPrefix.indexOf("_") + 1);
                    String value = env.get(envKey);

                    properties.setProperty(String.format("%s.%s", section, key), value);

                    envVarsRead++;
                    log.trace("Added {}.{}={} from environment", section, key, value);
                }
            }
        }

        return envVarsRead;
    }

    private String resolveEnvironmentVariables(final String value){
        String[] tokens = value.split("%");
        if (tokens.length == 1) {
            return value;
        }

        // iterate over the tokens
        String result = value;
        for(String token : tokens) {
            if (token == null || token.trim().length() == 0) {
                continue;
            }
            String envValue = getPropertyValue(token, null);
            if (envValue == null) {
                continue;
            }
            result = result.replaceAll("%" + token + "%", envValue);
        }

        return result;
    }

    public synchronized void load() throws MalformedURLException {
        applyConfigurationFromConfigService(properties);

        applyOverridesFromEnvironment(properties);

        initialized = true;
    }

    public synchronized void reload() throws MalformedURLException {

        Properties properties = new Properties();

        applyConfigurationFromConfigService(properties);

        applyOverridesFromEnvironment(properties);

        if (properties.isEmpty()) {
            log.debug("Failed to reload configuration from config service - ignoring reload attempt");
            return;
        }

        this.properties = properties;

        log.debug("Reloaded configuration from config service");
    }
}
