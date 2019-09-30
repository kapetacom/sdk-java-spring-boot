package com.blockware.spring.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.core.env.Environment;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.*;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Cluster service used in local (desktop) environments.
 *
 * Expects the blockware cluster service server application to be running (See blockctl)
 *
 * What's special about the local service is that we do not control the environment
 * so everything is being done "on-demand" from starting up databases to connecting routes.
 *
 * This is to ensure a simple integration with existing toolchains such as running services
 * from in an IDE or from the command line.
 *
 * We want to avoid any configuration needs (including env vars) from installing blockware locally to
 * running your services.
 */
class BlockwareClusterServiceLocal extends BlockwareClusterService {

    private static final Logger log = LoggerFactory.getLogger(BlockwareClusterServiceLocal.class);

    private static final String BLOCKWARE_CLUSTER_SERVICE_CONFIG_FILE = ".blockware/cluster-service.yml";

    private static final String BLOCKWARE_CLUSTER_SERVICE_DEFAULT_PORT = "35100";

    private static final String SERVER_PORT = "server.port";

    private static final String SERVER_PORT_TYPE = "rest";

    private static final String PROPERTY_USER_HOME = "user.home";

    private static final String DOCUMENT = "document";

    private static final String CONFIG_CLUSTER_PORT = "cluster.port";

    private int serverPort;

    private Properties clusterConfig;

    private ObjectMapper objectMapper = new ObjectMapper();

    BlockwareClusterServiceLocal(String blockRef, String systemId, String instanceId, Environment environment) {
        super(blockRef, systemId, instanceId, environment);
    }

    @Override
    public int getServerPort() {
        if (serverPort > 0) {
            return serverPort;
        }

        //Request server port from cluster service
        // - this is dynamic for local dev since we are running multiple services in the same
        //   port range

        final URL serverPortUrl = getProviderPort(SERVER_PORT_TYPE);

        try {
            serverPort = Integer.valueOf(sendGET(serverPortUrl));
            log.info("Got server port {} from config service: {}", serverPort, serverPortUrl);
        } catch (IOException e) {
            throw new RuntimeException("Failed to request server port for service: " + blockRef, e);
        }

        return serverPort;
    }



    @Override
    public void registerInstance(String instanceHealthPath) {
        final URL instancesUrl = getInstancesUrl();

        ApplicationPid pid = new ApplicationPid();
        final InstanceInfo instanceInfo = new InstanceInfo(pid.toString(), instanceHealthPath);

        try {
            sendPUT(instancesUrl, objectMapper.writeValueAsBytes(instanceInfo));
        } catch (IOException e) {
            throw new RuntimeException("Failed to register instance with cluster service", e);
        }

    }

    @Override
    public void instanceStopped() {
        final URL instancesUrl = getInstancesUrl();
        try {
            sendDELETE(instancesUrl);
        } catch (IOException e) {
            log.warn("Failed to unregister instance", e);
        }
    }

    @Override
    public String getServiceAddress(String serviceName, String portType) {

        final URL serviceClientUrl = getServiceClientUrl(serviceName, portType);

        try {
            return sendGET(serviceClientUrl);
        } catch (IOException e) {
            throw new RuntimeException("Failed to request client address port for service: " + serviceName, e);
        }
    }

    @Override
    public ResourceInfo getResourceInfo(String resourceType, String portType, String resourceName) {

        final URL resourceInfoUrl = getResourceInfoUrl(resourceType, portType, resourceName);

        try {
            final String json = sendGET(resourceInfoUrl);

            return objectMapper.readValue(json, ResourceInfo.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to request client address port for service: " + blockRef, e);
        }
    }


    @Override
    void applyConfiguration(Properties properties) throws ClusterServiceUnavailableException {

        final URL configServiceUrl = getConfigBaseUrl();

        applyConfigurationFromUrl(configServiceUrl, properties);

    }

    private Properties getClusterConfig() {
        if (clusterConfig != null) {
            return clusterConfig;
        }

        final String userHomeDir = System.getProperty(PROPERTY_USER_HOME);

        final File configFile = new File(userHomeDir + File.separator + BLOCKWARE_CLUSTER_SERVICE_CONFIG_FILE);

        clusterConfig = new Properties();

        if (!configFile.exists()) {
            return clusterConfig;
        }

        try (InputStream stream = new FileInputStream(configFile)) {
            readYAMLFromStream(stream, clusterConfig);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YAML configuration from: " + configFile, e);
        }

        return clusterConfig;
    }

    private void applyConfigurationFromUrl(URL configUrl, Properties properties) throws ClusterServiceUnavailableException {

        try (InputStream stream = sendRequestStream(configUrl, "GET")) {
            readYAMLFromStream(stream, properties);
        } catch (ConnectException ex) {
            throw new ClusterServiceUnavailableException("Failed to connect to cluster service. Verify that it's running and available here: " + getClusterServiceBaseUrl());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YAML configuration from: " + configUrl, e);
        }

        log.info("Loading configuration from {}", configUrl);
    }

    private void readYAMLFromStream(InputStream stream, Properties properties) throws IOException {
        final Yaml yaml = new Yaml();

        try (Reader reader = new UnicodeReader(stream)) {
            for (Object object : yaml.loadAll(reader)) {
                if (object == null) {
                    continue;
                }

                final Map<String, Object> stringObjectMap = asFlatMap(object);
                for(Map.Entry<String,Object> entry : stringObjectMap.entrySet()) {
                    properties.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        }
    }

    /**
     * Takes a nested data structure such as YAML and converts to a simple key/value
     */
    private Map<String, Object> asFlatMap(Object object) {
        return asFlatMap(object, "");
    }

    private Map<String, Object> asFlatMap(Object object, String parentPath) {
        // YAML can have numbers as keys
        Map<String, Object> result = new LinkedHashMap<>();
        if (!(object instanceof Map)) {
            // A document can be a text literal
            result.put(DOCUMENT, object);
            return result;
        }

        Map<Object, Object> map = (Map<Object, Object>) object;
        map.forEach((key, value) -> {

            String path = key.toString();
            if (!parentPath.isEmpty()) {
                path = parentPath + "." + key.toString();
            }

            if (value instanceof Map) {
                result.putAll(asFlatMap(value, path));
                return;
            }

            if (value instanceof String) {
                value = resolveEnvironmentVariables((String) value);
            }

            if (key instanceof CharSequence) {
                result.put(path, value);
            } else {
                // It has to be a map key in this case
                result.put(parentPath + "[" + key.toString() + "]", value);
            }
        });
        return result;
    }

    @Override
    public String getSourceId() {
        return getClusterServiceBaseUrl();
    }


    @Override
    public synchronized void load() throws Exception {
        super.load();

        resolveIdentity();

        final int serverPort = getServerPort();

        properties.put(SERVER_PORT, serverPort);
    }

    /**
     * Resolves identity based on available environment and local assets
     *
     * @throws IOException
     */
    private void resolveIdentity() throws IOException {

        URL url = getIdentityUrl();
        String identityJson = this.sendGET(url);

        final Identity identity = objectMapper.readValue(identityJson, Identity.class);

        log.info("Identity resolved: [system: {}] [instance: {}]", identity.systemId, identity.instanceId);

        this.systemId = identity.systemId;
        this.instanceId = identity.instanceId;
    }

    private String getClusterServiceBaseUrl() {

        final Properties clusterConfig = getClusterConfig();

        String clusterPort = clusterConfig.getProperty(CONFIG_CLUSTER_PORT, BLOCKWARE_CLUSTER_SERVICE_DEFAULT_PORT);

        return String.format("http://localhost:%s", clusterPort);
    }

    private URL getInstancesUrl() {
        String urlStr = getClusterServiceBaseUrl() + "/instances";
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to use configuration url: " + urlStr, e);
        }
    }

    private URL getConfigBaseUrl() {
        String urlStr = getClusterServiceBaseUrl() + "/config";
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to use configuration url: " + urlStr, e);
        }
    }

    private URL getProviderPort(String serviceType) {

        String subPath = String.format("/provides/%s", encode(serviceType));
        String urlStr = getConfigBaseUrl() + subPath;
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to use configuration url: " + urlStr, e);
        }
    }

    private URL getServiceClientUrl(String otherService, String serviceType) {

        String subPath = String.format("/consumes/%s/%s", encode(otherService), encode(serviceType));

        String urlStr = getConfigBaseUrl() + subPath;
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to use configuration url: " + urlStr, e);
        }
    }

    private URL getResourceInfoUrl(String operatorType, String portType, String resourceName) {

        String subPath = String.format("/consumes/resource/%s/%s/%s", encode(operatorType), encode(portType), encode(resourceName));
        String urlStr = getConfigBaseUrl() + subPath;
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to use configuration url: " + urlStr, e);
        }
    }

    private URL getIdentityUrl() {

        String urlStr = getConfigBaseUrl() + "/identity";
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to use configuration url: " + urlStr, e);
        }
    }

    private static String encode(String text) {
        try {
            return URLEncoder.encode(text.toLowerCase(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Identity {
        private String systemId;
        private String instanceId;

        public String getSystemId() {
            return systemId;
        }

        public void setSystemId(String systemId) {
            this.systemId = systemId;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }
    }
}
