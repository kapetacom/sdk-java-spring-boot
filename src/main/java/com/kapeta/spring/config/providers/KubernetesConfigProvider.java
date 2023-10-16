package com.kapeta.spring.config.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;


/**
 * Configuration for remotely running blocks
 *
 * Will read configuration from the ENV vars
 */
public class KubernetesConfigProvider implements KapetaConfigurationProvider {
    private static final Logger log = LoggerFactory.getLogger(KubernetesConfigProvider.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String systemId;
    private final Environment environment;
    private Map<String, String> instanceHosts;

    public KubernetesConfigProvider(String systemId, Environment environment) {
        this.systemId = systemId;
        this.environment = environment;
    }

    @Override
    public int getServerPort(String portType) {
        if (StringUtils.isEmpty(portType)) {
            portType = DEFAULT_SERVER_PORT_TYPE;
        }

        String envVar = "KAPETA_PROVIDER_PORT_%s".formatted(toEnvName(portType));
        String envVarValue = environment.getProperty(envVar);
        if (!StringUtils.isEmpty(envVarValue)) {
            return Integer.parseInt(envVarValue);
        }

        return 80; //We default to port 80
    }

    @Override
    public String getServerHost() {
        if (environment.containsProperty("KAPETA_PROVIDER_HOST")) {
            return environment.getProperty("KAPETA_PROVIDER_HOST");
        }
        //Any host within docker container
        return "0.0.0.0";
    }

    @Override
    public String getSystemId() {
        return systemId;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public String getServiceAddress(String serviceName, String portType) {
        var envVarName = "KAPETA_CONSUMER_SERVICE_%s_%s".formatted(toEnvName(serviceName), toEnvName(portType));
        return requireEnvVar(envVarName);
    }

    @Override
    public ResourceInfo getResourceInfo(String resourceType, String portType, String resourceName) {
        var envVarName = "KAPETA_CONSUMER_RESOURCE_%s_%s".formatted(toEnvName(resourceName), toEnvName(portType));

        var value = requireEnvVar(envVarName);
        try {
            return objectMapper.readValue(value, ResourceInfo.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse resource info from env var: %s".formatted(envVarName), e);
        }
    }

    @Override
    public Map<String,Object> getInstanceConfig() {
        var envVarName = "KAPETA_INSTANCE_CONFIG";
        if (environment.containsProperty(envVarName)) {
            var value = environment.getProperty(envVarName);
            try {
                return objectMapper.readValue(value, Map.class);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to parse configuration from env var: %s".formatted(envVarName), e);
            }
        } else {
            log.warn("Missing environment variable for instance configuration: KAPETA_INSTANCE_CONFIG");
        }

        return Collections.emptyMap();
    }

    @Override
    public String getInstanceHost(String instanceId) {
        if (this.instanceHosts == null) {
            var envVarName = "KAPETA_BLOCK_HOSTS";
            if (environment.containsProperty(envVarName)) {
                var value = environment.getProperty(envVarName);
                try {
                    this.instanceHosts = objectMapper.readValue(value, Map.class);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to parse instance hosts from env var: %s".formatted(envVarName), e);
                }
            } else {
                throw new IllegalStateException("Environment variable KAPETA_BLOCK_HOSTS not found. Could not resolve instance host.");
            }
        }

        if (this.instanceHosts.containsKey(instanceId)) {
            return this.instanceHosts.get(instanceId);
        }

        throw new IllegalStateException("Unknown instance id when resolving host: %s".formatted(instanceId));
    }

    @Override
    public String getInstanceProviderUrl(String instanceId, String portType, String resourceName) {
        //TODO: Implement this (KAP-764)
        throw new RuntimeException("Method not implemented.");
    }

    @Override
    public String getProviderId() {
        return "kubernetes";
    }

    private String toEnvName(String name) {
        return name.toUpperCase().trim().replaceAll("[.,-]", "_");
    }

    private String requireEnvVar(String envVarName) {
        var envVarValue = environment.getProperty(envVarName);
        if (!StringUtils.isEmpty(envVarValue)) {
            return envVarValue;
        }

        throw new IllegalStateException("Missing environment variable for internal resource: %s".formatted(envVarName));
    }

}
