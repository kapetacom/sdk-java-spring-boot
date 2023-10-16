package com.kapeta.spring.config.providers;

import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

public interface KapetaConfigurationProvider {
    String DEFAULT_SERVER_PORT_TYPE = "rest";

    default int getServerPort() {
        return getServerPort(DEFAULT_SERVER_PORT_TYPE);
    }

    /**
     * Gets the primary server port for this service
     */
    int getServerPort(String portType);

    String getServerHost();

    String getSystemId();

    Environment getEnvironment();

    /**
     * Gets the remote address for a given service name and port type.
     *
     * E.g.: getServiceAddress("users" , "rest");
     *
     * @param serviceName
     * @param portType
     * @return
     */
    String getServiceAddress(String serviceName, String portType);

    /**
     * Gets resource information for a given resource type. This is used for getting non-block
     * dependency information such as databases, MQ's and more.
     *
     * E.g.: getResourceInfo("kapeta/resource-type-postgresql" , "postgres");
     *
     * @param resourceType
     * @param portType
     * @return
     */
    ResourceInfo getResourceInfo(String resourceType, String portType, String name);

    String getInstanceHost(String instanceId);

    String getInstanceProviderUrl(String instanceId, String portType, String resourceName);

    Map<String,Object> getInstanceConfig() throws Exception;

    /**
     * Get unique source ID for this configuration source
     * @return
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
