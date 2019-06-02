package com.blockware.spring.config;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.UrlResource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Configuration source intended for use only in local environments.
 *
 * Expects the blockware config management application to be running
 */
class BlockwareConfigSourceLocal extends BlockwareConfigSource {

    private static final Logger log = LoggerFactory.getLogger(BlockwareConfigSourceLocal.class);

    private static final String BLOCKWARE_CONFIG_SERVICE = "BLOCKWARE_CONFIG_SERVICE";

    private static final String BLOCKWARE_CONFIG_SERVICE_DEFAULT = "http://localhost:30033/";

    private static final String SERVER_PORT = "server.port";

    private int serverPort;

    BlockwareConfigSourceLocal(String serviceName, Environment environment) {
        super(serviceName, environment);
    }

    @Override
    public int getServerPort() {
        if (serverPort > 0) {
            return serverPort;
        }

        //Request server port from configuration server
        // - this is dynamic for local dev since we are running multiple services in the same
        //   port range

        final URL serverPortUrl = getServerPortUrl();

        try {
            serverPort = requestServerPort(serverPortUrl);
            log.info("Got server port {} from config service: {}", serverPort, serverPortUrl);
        } catch (IOException e) {
            throw new RuntimeException("Failed to request server port for service: " + serviceName, e);
        }

        return serverPort;
    }

    private Integer requestServerPort(final URL url) throws IOException {
        final String content = IOUtils.toString(url, StandardCharsets.UTF_8);

        return Integer.valueOf(content);
    }

    @Override
    public String getClientAddress(String serviceName, String serviceType) {

        final URL serviceClientUrl = getServiceClientUrl(serviceName, serviceType);

        try {
            return requestServiceClient(serviceClientUrl);
        } catch (IOException e) {
            throw new RuntimeException("Failed to request client address port for service: " + serviceName, e);
        }
    }

    private String requestServiceClient(final URL url) throws IOException {
        return IOUtils.toString(url, StandardCharsets.UTF_8);
    }



    @Override
    void applyConfiguration(Properties properties) throws Exception {

        final URL configServiceUrl = getConfigServiceUrl();

        applyConfigurationFromUrl(configServiceUrl, properties);

        final int serverPort = getServerPort();

        properties.put(SERVER_PORT, serverPort);
    }

    private void applyConfigurationFromUrl(URL configUrl, Properties properties) {

        final UrlResource urlResource = new UrlResource(configUrl);

        final Yaml yaml = new Yaml();

        try(Reader reader = new UnicodeReader(urlResource.getInputStream())) {
            for (Object object : yaml.loadAll(reader)) {
                if (object == null) {
                    continue;
                }

                final Map<String, Object> stringObjectMap = asMap(object);

                properties.putAll(stringObjectMap);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YAML configuration from: " + configUrl, e);
        }

        log.info("Loading configuration from {}", configUrl);
    }

    private Map<String, Object> asMap(Object object) {
        return asMap(object, "");
    }

    private Map<String, Object> asMap(Object object, String parentPath) {
        // YAML can have numbers as keys
        Map<String, Object> result = new LinkedHashMap<>();
        if (!(object instanceof Map)) {
            // A document can be a text literal
            result.put("document", object);
            return result;
        }

        Map<Object, Object> map = (Map<Object, Object>) object;
        map.forEach((key, value) -> {

            String path = key.toString();
            if (!parentPath.isEmpty()) {
                path = parentPath + "." + key.toString();
            }

            if (value instanceof Map) {
                result.putAll(asMap(value, path ));
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
    String getSourceId() {
        return getConfigServiceUrl().toExternalForm();
    }

    private String getConfigServiceBaseUrl()  {

        String hostname = getPropertyValue(BLOCKWARE_CONFIG_SERVICE, BLOCKWARE_CONFIG_SERVICE_DEFAULT);

        if (!hostname.endsWith("/")) {
            hostname += "/";
        }

        return hostname + "services/" + serviceName.toLowerCase();
    }

    private URL getConfigServiceUrl()  {
        String urlStr = getConfigServiceBaseUrl() + "/config";
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to use configuration url: " + urlStr, e);
        }
    }

    private URL getServerPortUrl()  {

        String urlStr = getConfigServiceBaseUrl() + "/port";
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to use configuration url: " + urlStr, e);
        }
    }

    private URL getServiceClientUrl(String otherService, String serviceType)  {

        String urlStr = getConfigServiceBaseUrl() + "/client/" + otherService.toLowerCase() + "/types/" + serviceType;
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to use configuration url: " + urlStr, e);
        }
    }
}
