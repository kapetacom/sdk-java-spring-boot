/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */

package com.kapeta.spring.config.providers;

import com.kapeta.schemas.entity.BlockDefinition;
import com.kapeta.schemas.entity.Connection;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface KapetaConfigurationProvider {
    String DEFAULT_SERVER_PORT_TYPE = "rest";

    /**
     * Gets the server port for this service
     */
    default int getServerPort() {
        return getServerPort(DEFAULT_SERVER_PORT_TYPE);
    }

    /**
     * Gets the server port for this service and port type
     */
    int getServerPort(String portType);

    /**
     * Gets the address where this service should bind to
     */
    String getServerHost();

    /**
     * Get the system ID which is the same as the plan reference
     */
    String getSystemId();

    /**
     * Get the spring environment used for this config provider
     */
    Environment getEnvironment();

    /**
     * Gets the remote address for a given service name and port type.
     * <p>
     * E.g.: getServiceAddress("users" , "rest");
     */
    String getServiceAddress(String serviceName, String portType);

    /**
     * Gets resource information for a given resource type. This is used for getting non-block
     * dependency information such as databases, MQ's and more.
     * <p>
     * E.g.: getResourceInfo("kapeta/resource-type-postgresql" , "postgres", "mydb");
     */
    ResourceInfo getResourceInfo(String resourceType, String portType, String name);

    /**
     * Gets the host name for a given instance ID
     */
    String getInstanceHost(String instanceId);

    /**
     * Gets the configuration for the current instance
     */
    Map<String, Object> getInstanceConfig() throws Exception;

    /**
     * Get unique source ID for this configuration source
     */
    String getProviderId();

    <BlockType> BlockInstanceDetails<BlockType> getInstanceForConsumer(String resourceName, Class<BlockType> clz) throws IOException;

    default BlockInstanceDetails<BlockDefinition> getInstanceForConsumer(String resourceName) throws IOException {
        return getInstanceForConsumer(resourceName, BlockDefinition.class);
    }

    <Options, Credentials> InstanceOperator<Options, Credentials> getInstanceOperator(String instanceId, Class<Options> optionsClass, Class<Credentials> credentialsClass) throws IOException;

    default <Options> InstanceOperator<Options, DefaultCredentials> getInstanceOperator(String instanceId, Class<Options> optionsClass) throws IOException {
        return getInstanceOperator(instanceId, optionsClass, DefaultCredentials.class);
    }

    default InstanceOperator<DefaultOptions, DefaultCredentials> getInstanceOperator(String instanceId) throws IOException {
        return getInstanceOperator(instanceId, DefaultOptions.class);
    }

    <BlockType> List<BlockInstanceDetails<BlockType>> getInstancesForProvider(String resourceName, Class<BlockType> clz) throws IOException;

    default List<BlockInstanceDetails<BlockDefinition>> getInstancesForProvider(String resourceName) throws IOException {
        return getInstancesForProvider(resourceName, BlockDefinition.class);
    }

    class DefaultOptions extends HashMap<String, String> {
    }

    class DefaultCredentials {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    class BlockInstanceDetails<BlockType> {
        private String instanceId;

        private BlockType block;

        private List<Connection> connections = new ArrayList<>();

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }

        public BlockType getBlock() {
            return block;
        }

        public void setBlock(BlockType block) {
            this.block = block;
        }

        public List<Connection> getConnections() {
            return connections;
        }

        public void setConnections(List<Connection> connections) {
            this.connections = connections;
        }
    }

    class InstanceInfo {

        private String pid;

        private String health;

        public InstanceInfo(String pid, String health) {
            this.pid = pid;
            this.health = health;
        }

        public String getPid() {
            return pid;
        }

        public String getHealth() {
            return health;
        }
    }

    class ResourceInfo {

        private String host;

        private String port;

        private String type;

        private String protocol;

        private String resource;

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

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }
    }


    class InstanceOperatorPort {
        private String protocol;
        private int port;

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    class InstanceOperator<Options,Credentials> {
        private String hostname;
        private Map<String, InstanceOperatorPort> ports = new HashMap<>();
        private String path;
        private String query;
        private String hash;
        private Credentials credentials;
        private Options options;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public Map<String, InstanceOperatorPort> getPorts() {
            return ports;
        }

        public void setPorts(Map<String, InstanceOperatorPort> ports) {
            this.ports = ports;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public Credentials getCredentials() {
            return credentials;
        }

        public void setCredentials(Credentials credentials) {
            this.credentials = credentials;
        }

        public Options getOptions() {
            return options;
        }

        public void setOptions(Options options) {
            this.options = options;
        }
    }
}
