package com.kapeta.spring.config.providers;

import org.springframework.core.env.Environment;

import java.util.HashMap;
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
    Map<String,Object> getInstanceConfig() throws Exception;

    /**
     * Get unique source ID for this configuration source
     */
    String getProviderId();


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
}
