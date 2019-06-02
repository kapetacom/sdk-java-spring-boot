package com.blockware.spring.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

abstract public class BlockwareConfigSource extends PropertySource<Object> {

    private static final Logger log = LoggerFactory.getLogger(BlockwareConfigSource.class);

    public static final String BLOCKWARE_CONFIG_NAME = "BLOCKWARE_CONFIG";

    protected final String serviceName;

    protected final Environment environment;

    protected Properties properties = new Properties();

    private boolean initialized;
    
    public BlockwareConfigSource(String serviceName, Environment environment) {
        super(BLOCKWARE_CONFIG_NAME);
        this.environment = environment;
        this.serviceName = serviceName;
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    public abstract int getServerPort();

    public abstract String getClientAddress(String serviceName, String serviceType);

    abstract void applyConfiguration(Properties properties) throws Exception;

    abstract String getSourceId();

    private void applyConfigurationFromConfigService(Properties properties) throws Exception {
        applyConfiguration(properties);

        if (initialized) {
            log.debug("Read config from source: {}", getSourceId());
        } else {
            log.info("Read config from source: {}", getSourceId());
        }

        int envVars = applyOverridesFromEnvironment();
        log.trace("Read {} 'BLOCKWARE_<section>_<key>=<value>' environment variables (overwrites values from files)", envVars);
    }

    protected String getPropertyValue(String key, String defaultValue) {
        return BlockwareConfigSourceLoader.getSystemConfiguration(environment, key, key.toUpperCase(), defaultValue);
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

    protected String resolveEnvironmentVariables(final String value) {
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

    public synchronized void load() throws Exception {
        applyConfigurationFromConfigService(properties);

        applyOverridesFromEnvironment(properties);

        initialized = true;
    }

    public synchronized void reload() throws Exception {

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
