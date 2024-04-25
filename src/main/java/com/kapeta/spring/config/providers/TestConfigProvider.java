/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */

package com.kapeta.spring.config.providers;

import com.kapeta.spring.config.providers.types.BlockInstanceDetails;
import com.kapeta.spring.config.providers.types.InstanceOperator;
import com.kapeta.spring.config.providers.types.ResourceInfo;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import java.util.*;


/**
 * Configuration for tests
 */
public class TestConfigProvider implements KapetaConfigurationProvider {
    private int serverPort = 80;
    private String serverHost = "0.0.0.0";
    private String instanceId = UUID.randomUUID().toString();
    private String blockRef = "test/block:0.0.1";
    private String systemId = "test/system:0.0.1";
    private ConfigurableEnvironment environment;

    private Map<String, Map<String, String>> serviceAddresses = new HashMap<>();
    private Map<String, Map<String, ResourceInfo>> resourceInfos = new HashMap<>();
    private Map<String, Object> instanceConfig = new HashMap<>();
    private Map<String, String> instanceHosts = new HashMap<>();
    private Map<String, InstanceOperator> instanceOperators = new HashMap<>();
    private Map<String, BlockInstanceDetails> consumerInstances = new HashMap<>();
    private Map<String, List<BlockInstanceDetails>> providerInstances = new HashMap<>();

    public TestConfigProvider(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    public TestConfigProvider() {
        this(new StandardEnvironment());
    }

    public TestConfigProvider withEnvironment(ConfigurableEnvironment environment) {
        this.environment = environment;
        return this;
    }


    public TestConfigProvider withServerPort(int serverPort) {
        this.serverPort = serverPort;
        return this;
    }

    public TestConfigProvider withInstanceId(String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    public TestConfigProvider withBlockRef(String blockRef) {
        this.blockRef = blockRef;
        return this;
    }

    public TestConfigProvider withServerHost(String serverHost) {
        this.serverHost = serverHost;
        return this;
    }

    public TestConfigProvider withSystemId(String systemId) {
        this.systemId = systemId;
        return this;
    }

    public TestConfigProvider withServiceAddress(String serviceName, String portType, String address) {
        serviceAddresses.computeIfAbsent(serviceName, k -> new HashMap<>()).put(portType, address);
        return this;
    }

    public TestConfigProvider withResourceInfo(String resourceName, String portType, ResourceInfo resourceInfo) {
        resourceInfos.computeIfAbsent(resourceName, k -> new HashMap<>()).put(portType, resourceInfo);
        return this;
    }

    public TestConfigProvider withInstanceConfig(String key, Object value) {
        instanceConfig.put(key, value);
        return this;
    }

    public TestConfigProvider withInstanceConfig(Map<String, Object> instanceConfig) {
        this.instanceConfig = instanceConfig;
        return this;
    }

    public TestConfigProvider withInstanceHost(String instanceId, String host) {
        instanceHosts.put(instanceId, host);
        return this;
    }

    public TestConfigProvider withInstanceOperator(String instanceId, InstanceOperator instanceOperator) {
        instanceOperators.put(instanceId, instanceOperator);
        return this;
    }

    public TestConfigProvider withConsumerInstance(String resourceName, BlockInstanceDetails consumerInstance) {
        consumerInstances.put(resourceName, consumerInstance);
        return this;
    }

    public TestConfigProvider withProviderInstance(String resourceName, BlockInstanceDetails providerInstance) {
        providerInstances.computeIfAbsent(resourceName, k -> new ArrayList<>()).add(providerInstance);
        return this;
    }

    public TestConfigProvider withProviderInstances(String resourceName, List<BlockInstanceDetails> providerInstances) {
        this.providerInstances.put(resourceName, providerInstances);
        return this;
    }

    @Override
    public int getServerPort(String portType) {
        return serverPort;
    }

    @Override
    public String getServerHost() {
        return serverHost;
    }

    @Override
    public String getSystemId() {
        return systemId;
    }

    @Override
    public ConfigurableEnvironment getEnvironment() {
        return environment;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getBlockRef() {
        return blockRef;
    }

    @Override
    public String getServiceAddress(String serviceName, String portType) {
        if (serviceAddresses.containsKey(serviceName) &&
                serviceAddresses.get(serviceName).containsKey(portType)) {
            return serviceAddresses.get(serviceName).get(portType);
        }
        throw new IllegalArgumentException("Service not found: " + serviceName + " portType: " + portType);
    }

    @Override
    public ResourceInfo getResourceInfo(String resourceType, String portType, String resourceName) {
        if (resourceInfos.containsKey(resourceName) &&
                resourceInfos.get(resourceName).containsKey(portType)) {
            return resourceInfos.get(resourceName).get(portType);
        }
        throw new IllegalArgumentException("Resource not found: " + resourceName + " portType: " + portType);
    }

    @Override
    public Map<String, Object> getInstanceConfig() {
        return instanceConfig;
    }

    @Override
    public String getInstanceHost(String instanceId) {
        if (instanceHosts.containsKey(instanceId)) {
            return instanceHosts.get(instanceId);
        }

        throw new IllegalArgumentException("Instance not found: " + instanceId);
    }

    @Override
    public String getProviderId() {
        return "test";
    }

    @Override
    public <Options, Credentials> InstanceOperator<Options, Credentials> getInstanceOperator(String instanceId, Class<Options> optionsClass, Class<Credentials> credentialsClass) {
        if (instanceOperators.containsKey(instanceId)) {
            return (InstanceOperator<Options, Credentials>) instanceOperators.get(instanceId);
        }
        throw new IllegalArgumentException("Instance not found: " + instanceId);
    }

    @Override
    public <BlockType> BlockInstanceDetails<BlockType> getInstanceForConsumer(String resourceName, Class<BlockType> clz) {
        if (consumerInstances.containsKey(resourceName)) {
            return (BlockInstanceDetails<BlockType>) consumerInstances.get(resourceName);
        }
        throw new IllegalArgumentException("Instance not found for consumer resource: " + resourceName);
    }


    @Override
    public <BlockType> List<BlockInstanceDetails<BlockType>> getInstancesForProvider(String resourceName, Class<BlockType> clz) {
        if (providerInstances.containsKey(resourceName)) {
            return new ArrayList(providerInstances.get(resourceName));
        }
        throw new IllegalArgumentException("Instances not found for provider resource: " + resourceName);
    }

    public interface TestConfigurationAdjuster {
        void adjust(TestConfigProvider provider);
    }


}
