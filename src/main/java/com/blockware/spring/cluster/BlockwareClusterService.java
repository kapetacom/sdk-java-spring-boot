package com.blockware.spring.cluster;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Base class for cluster service handling.
 *
 * Each environment (local, kubernetes, etc.) has an implementation that is
 * specific to the environment and each have different expectations based on that environment.
 *
 * Common for all of them is that this is the central class for getting information
 * about the blockware system in which this service is running.
 *
 * What is different is how that information is retrieved.
 *
 */
abstract public class BlockwareClusterService extends PropertySource<Object> {
    private static final Logger log = LoggerFactory.getLogger(BlockwareClusterService.class);

    public static final String HEADER_BLOCKWARE_BLOCK = "X-Blockware-Block";

    public static final String HEADER_BLOCKWARE_SYSTEM = "X-Blockware-System";

    public static final String HEADER_BLOCKWARE_INSTANCE = "X-Blockware-Instance";

    public static final String BLOCKWARE_CONFIG_NAME = "BLOCKWARE_CONFIG";

    protected final String blockRef;

    protected final Environment environment;

    protected String systemId;

    protected String instanceId;

    protected Properties properties = new Properties();

    private boolean initialized;
    
    public BlockwareClusterService(String blockRef, String systemId, String instanceId, Environment environment) {
        super(BLOCKWARE_CONFIG_NAME);
        this.environment = environment;
        this.blockRef = blockRef;
        this.systemId = systemId;
        this.instanceId = instanceId;
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    public String getBlockRef() {
        return blockRef;
    }

    public String getSystemId() {
        return systemId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Gets the primary server port for this service
     */
    public abstract int getServerPort();

    /**
     * Gets the remote address for a given service name and port type.
     *
     * E.g.: getServiceAddress("users" , "rest");
     *
     * @param serviceName
     * @param portType
     * @return
     */
    public abstract String getServiceAddress(String serviceName, String portType);

    /**
     * Gets resource information for a given resource type. This is used for getting non-block
     * dependency information such as databases, MQ's and more.
     *
     * E.g.: getResourceInfo("sqldb.blockware.com/v1/postgresql" , "postgres");
     *
     * @param resourceType
     * @param portType
     * @return
     */
    public abstract ResourceInfo getResourceInfo(String resourceType, String portType, String name);

    /**
     * Applies configuration values to the given properties object
     *
     * @param properties
     * @throws Exception
     */
    abstract void applyConfiguration(Properties properties) throws Exception;

    /**
     * Get unique source ID for this cluster service
     * @return
     */
    public abstract String getSourceId();

    /**
     * Helper method for sending a GET request to a URL which will include the proper headers etc.
     *
     * Returns the response body as a string
     *
     */
    protected String sendGET(final URL url) throws IOException {

        try (InputStream stream = sendGETStream(url)) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }

    /**
     * Helper method for sending a GET request to a URL which will include the proper headers etc.
     *
     * Returns the response body as a stream
     *
     */
    protected InputStream sendGETStream(final URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        connection.addRequestProperty(HEADER_BLOCKWARE_BLOCK, blockRef);
        connection.addRequestProperty(HEADER_BLOCKWARE_SYSTEM, systemId);
        connection.addRequestProperty(HEADER_BLOCKWARE_INSTANCE, instanceId);

        return connection.getInputStream();
    }

    protected String getPropertyValue(String key, String defaultValue) {
        return BlockwareClusterServiceInitializer.getSystemConfiguration(environment, key, key.toUpperCase(), defaultValue);
    }

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


    public static class ResourceInfo {

        private String host;

        private String port;

        private String type;

        private String protocol;

        private Map<String, Object> options = new HashMap<>();

        private Map<String, String> credentials = new HashMap<>();

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public Map<String, Object> getOptions() {
            return options;
        }

        public void setOptions(Map<String, Object> options) {
            this.options = options;
        }

        public Map<String, String> getCredentials() {
            return credentials;
        }

        public void setCredentials(Map<String, String> credentials) {
            this.credentials = credentials;
        }

    }
}
