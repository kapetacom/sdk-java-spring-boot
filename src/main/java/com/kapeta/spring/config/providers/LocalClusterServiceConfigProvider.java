/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */

package com.kapeta.spring.config.providers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kapeta.schemas.entity.BlockDefinition;
import com.kapeta.schemas.entity.BlockInstance;
import com.kapeta.schemas.entity.Connection;
import com.kapeta.schemas.entity.Plan;
import com.kapeta.spring.config.SimpleHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.cglib.core.Block;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kapeta.spring.config.ConfigUtils.getPropertiesFromYAML;
import static com.kapeta.spring.config.KapetaDefaultConfig.createDefaultObjectMapper;


/**
 * Cluster service used in local (desktop) environments.
 * <p>
 * Expects the kapeta cluster service server application to be running
 * <p>
 * What's special about the local service is that we do not control the environment
 * so everything is being done "on-demand" from starting up databases to connecting routes.
 * <p>
 * This is to ensure a simple integration with existing toolchains such as running services
 * from in an IDE or from the command line.
 * <p>
 * We want to avoid any configuration needs (including env vars) from installing kapeta locally to
 * running your services.
 */
public class LocalClusterServiceConfigProvider implements KapetaConfigurationProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalClusterServiceConfigProvider.class);

    private static final String KAPETA_CLUSTER_SERVICE_CONFIG_FILE = ".kapeta/cluster-service.yml";

    private static final String KAPETA_CLUSTER_SERVICE_DEFAULT_PORT = "35100";

    private static final String KAPETA_CLUSTER_SERVICE_DEFAULT_HOST = "127.0.0.1";

    private static final String PROPERTY_USER_HOME = "user.home";

    private static final String CONFIG_CLUSTER_PORT = "cluster.port";

    private static final String CONFIG_CLUSTER_HOST = "cluster.host";

    private final Environment environment;

    private final ObjectMapper objectMapper = createDefaultObjectMapper();

    private final SimpleHttpClient httpClient;

    private Properties clusterConfig;

    public LocalClusterServiceConfigProvider(String blockRef, String systemId, String instanceId, Environment environment) throws IOException {
        this.httpClient = new SimpleHttpClient(blockRef, systemId, instanceId);
        this.environment = environment;

        //Locally we need to ask the local cluster service about our identity
        this.resolveIdentity();
    }

    @Override
    public int getServerPort(String portType) {

        //Request server port from cluster service
        // - this is dynamic for local dev since we are running multiple services in the same
        //   port range

        var envVarName = "KAPETA_LOCAL_SERVER_PORT_%s".formatted(portType.toUpperCase());
        var envVarValue = environment.getProperty(envVarName);

        if (StringUtils.hasText(envVarValue)) {
            return Integer.parseInt(envVarValue);
        }

        final String serverPortUrl = getProviderPortUrl(portType);

        try {
            var serverPort = Integer.parseInt(httpClient.sendGET(serverPortUrl));
            log.info("Got server port {} from config service: {}", serverPort, serverPortUrl);
            return serverPort;
        } catch (IOException e) {
            throw new RuntimeException("Failed to request server port for service: " + httpClient.getBlockRef(), e);
        }
    }

    @Override
    public String getServerHost() {
        if (environment.containsProperty("KAPETA_LOCAL_SERVER")) {
            return environment.getProperty("KAPETA_LOCAL_SERVER");
        }

        return "127.0.0.1";
    }

    @Override
    public String getSystemId() {
        return httpClient.getSystemId();
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public String getServiceAddress(String serviceName, String portType) {

        final String serviceClientUrl = getServiceClientUrl(serviceName, portType);

        try {
            return httpClient.sendGET(serviceClientUrl);
        } catch (IOException e) {
            throw new RuntimeException("Failed to request client address port for service: " + serviceName, e);
        }
    }

    @Override
    public ResourceInfo getResourceInfo(String resourceType, String portType, String resourceName) {

        final String resourceInfoUrl = getResourceInfoUrl(resourceType, portType, resourceName);

        try {
            final String json = httpClient.sendGET(resourceInfoUrl);

            return objectMapper.readValue(json, ResourceInfo.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to request client address port for service: " + httpClient.getBlockRef(), e);
        }
    }

    @Override
    public String getInstanceHost(String instanceId) {
        var url = this.getInstanceHostUrl(instanceId);
        try {
            return httpClient.sendGET(url);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get instance provider url", e);
        }
    }

    public void onInstanceStarted(String instanceHealthPath) {
        final String instancesUrl = getInstanceUrl();

        ApplicationPid pid = new ApplicationPid();
        final InstanceInfo instanceInfo = new InstanceInfo(pid.toString(), instanceHealthPath);

        try {
            httpClient.sendPUT(instancesUrl, objectMapper.writeValueAsBytes(instanceInfo));
        } catch (IOException e) {
            throw new RuntimeException("Failed to register instance with cluster service", e);
        }
    }

    public void onInstanceStopped() {
        final String instancesUrl = getInstanceUrl();
        try {
            httpClient.sendDELETE(instancesUrl);
        } catch (IOException e) {
            log.warn("Failed to unregister instance", e);
        }
    }

    @Override
    public Map<String,Object> getInstanceConfig() throws IOException {
        var url = this.getInstanceConfigUrl();

        String response = httpClient.sendGET(url);

        if (!StringUtils.hasText(response)) {
            return new HashMap<>();
        }

        return objectMapper.readValue(response, Map.class);
    }


    @Override
    public String getProviderId() {
        return getClusterServiceBaseUrl();
    }

    @Override
    public <BlockType> BlockInstanceDetails<BlockType> getInstanceForConsumer(String resourceName, Class<BlockType> clz) throws IOException {
        Plan plan = getPlan();
        if (plan == null) {
            throw new RuntimeException("Could not find plan");
        }

        String instanceId = httpClient.getInstanceId();
        var connection = plan.getSpec().getConnections().stream()
                .filter(conn -> conn.getConsumer().getBlockId().equals(instanceId)
                        && conn.getConsumer().getResourceName().equals(resourceName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find connection for consumer " + resourceName));

        var instance = plan.getSpec().getBlocks().stream()
                .filter(b -> b.getId().equals(connection.getProvider().getBlockId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find instance " + connection.getProvider().getBlockId() + " in plan"));

        var block = getAsset(instance.getBlock().getRef(), clz);
        if (block == null) {
            throw new RuntimeException("Could not find block " + instance.getBlock().getRef() + " in plan");
        }

        var details = new BlockInstanceDetails<BlockType>();
        details.setInstanceId(connection.getProvider().getBlockId());
        details.setConnections(List.of(connection));
        details.setBlock(block);

        return details;
    }

    @Override
    public <BlockType> List<BlockInstanceDetails<BlockType>> getInstancesForProvider(String resourceName, Class<BlockType> clz) throws IOException {
        Plan plan = getPlan();
        if (plan == null) {
            throw new RuntimeException("Could not find plan");
        }

        String instanceId = httpClient.getInstanceId();
        var blockDetails = new HashMap<String, BlockInstanceDetails<BlockType>>();

        var connections = plan.getSpec().getConnections().stream()
                .filter(connection -> connection.getProvider().getBlockId().equals(instanceId)
                        && connection.getProvider().getResourceName().equals(resourceName))
                .toList();

        for (Connection connection : connections) {
            String blockInstanceId = connection.getConsumer().getBlockId();
            if (blockDetails.containsKey(blockInstanceId)) {
                blockDetails.get(blockInstanceId).getConnections().add(connection);
                continue;
            }

            var instance = plan.getSpec().getBlocks().stream()
                    .filter(b -> b.getId().equals(blockInstanceId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find instance " + blockInstanceId + " in plan"));

            var block = getAsset(instance.getBlock().getRef(), clz);
            if (block == null) {
                throw new RuntimeException("Could not find block " + instance.getBlock().getRef() + " in plan");
            }

            var details = blockDetails.get(blockInstanceId);
            if (details == null) {
                details = new BlockInstanceDetails<>();
                details.setInstanceId(blockInstanceId);
                details.setBlock(block);
                details.setConnections(new ArrayList<>());
                blockDetails.put(blockInstanceId, details);

            }
            details.getConnections().add(connection);
        }

        return new ArrayList<>(blockDetails.values());
    }

    public Plan getPlan() throws IOException {
        return getAsset(getSystemId(), Plan.class);
    }

    public BlockDefinition getBlock(String ref) throws IOException {
        return getAsset(ref, BlockDefinition.class);
    }

    public <AssetType> AssetType getAsset(String ref, Class<AssetType> clz) throws IOException {
        String url = getAssetReadUrl(ref);
        var response = httpClient.sendGET(url);
        if (!StringUtils.hasText(response)) {
            return null;
        }
        var type = new TypeReference<AssetWrapper<AssetType>>() {};
        return objectMapper.readValue(response, type).getData();
    }


    @Override
    public <Options, Credentials> InstanceOperator<Options, Credentials> getInstanceOperator(String instanceId, Class<Options> optionsClass, Class<Credentials> credentialsClass) throws IOException {
        var url = getInstanceOperatorUrl(instanceId);
        String response = httpClient.sendGET(url);

        if (!StringUtils.hasText(response)) {
            return null;
        }

        var typeRef = new TypeReference<InstanceOperator<Options, Credentials>>() {};

        return objectMapper.readValue(response, typeRef);
    }

    private String getInstanceOperatorUrl(String instanceId) {
        String subPath = String.format("/operator/%s", encode(instanceId));
        return getConfigBaseUrl() + subPath;
    }

    private String getInstanceConfigUrl() {
        var subPath = "/config/instance";
        return this.getClusterServiceBaseUrl() + subPath;
    }


    private String getInstanceHostUrl(String instanceId) {
        var subPath = Stream.of(
                        httpClient.getSystemId(),
                        instanceId,
                        "address",
                        "public"
                )
                .map(LocalClusterServiceConfigProvider::encode)
                .collect(Collectors.joining("/"));

        return this.getInstanceUrl() + '/' + subPath;
    }

    private Properties getLocalClusterConfig() {
        if (clusterConfig != null) {
            return clusterConfig;
        }

        final String userHomeDir = System.getProperty(PROPERTY_USER_HOME);

        final File configFile = new File(userHomeDir + File.separator + KAPETA_CLUSTER_SERVICE_CONFIG_FILE);

        clusterConfig = getPropertiesFromYAML(configFile, environment);

        return clusterConfig;
    }

    /**
     * Resolves identity based on available environment and local assets
     *
     * @throws IOException
     */
    private void resolveIdentity() throws IOException {

        String url = getIdentityUrl();
        String identityJson = httpClient.sendGET(url);

        final Identity identity = objectMapper.readValue(identityJson, Identity.class);

        log.info("Identity resolved: [system: {}] [instance: {}]", identity.systemId, identity.instanceId);

        httpClient.setSystemId(identity.systemId);
        httpClient.setInstanceId(identity.instanceId);
    }

    private String getClusterServiceBaseUrl() {

        final Properties clusterConfig = getLocalClusterConfig();

        String clusterPort = clusterConfig.getProperty(CONFIG_CLUSTER_PORT, KAPETA_CLUSTER_SERVICE_DEFAULT_PORT);

        String clusterHost = clusterConfig.getProperty(CONFIG_CLUSTER_HOST, KAPETA_CLUSTER_SERVICE_DEFAULT_HOST);

        if (environment.containsProperty("KAPETA_LOCAL_CLUSTER_HOST")) {
            clusterHost = environment.getProperty("KAPETA_LOCAL_CLUSTER_HOST");
        }

        if (environment.containsProperty("KAPETA_LOCAL_CLUSTER_PORT")) {
            clusterPort = environment.getProperty("KAPETA_LOCAL_CLUSTER_PORT");
        }

        return String.format("http://%s:%s", clusterHost, clusterPort);
    }

    private String getInstanceUrl() {
        return getClusterServiceBaseUrl() + "/instances";
    }

    private String getConfigBaseUrl() {
        return getClusterServiceBaseUrl() + "/config";
    }

    private String getAssetReadUrl(String ref) {
        String subPath = String.format("/assets/read?ref=%s", encode(ref));

        return this.getClusterServiceBaseUrl() + subPath;
    }

    private String getProviderPortUrl(String serviceType) {

        String subPath = String.format("/provides/%s", encode(serviceType));
        return getConfigBaseUrl() + subPath;
    }

    private String getServiceClientUrl(String otherService, String serviceType) {

        String subPath = String.format("/consumes/%s/%s", encode(otherService), encode(serviceType));

        return getConfigBaseUrl() + subPath;
    }

    private String getResourceInfoUrl(String operatorType, String portType, String resourceName) {

        String subPath = String.format("/consumes/resource/%s/%s/%s", encode(operatorType), encode(portType), encode(resourceName));
        return getConfigBaseUrl() + subPath;
    }

    private String getIdentityUrl() {

        return getConfigBaseUrl() + "/identity";
    }

    private static String encode(String text) {
        try {
            return URLEncoder.encode(text.toLowerCase(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Identity {
        public String systemId;
        public String instanceId;
    }

    private static class AssetWrapper<T> {
        private T data;

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}
