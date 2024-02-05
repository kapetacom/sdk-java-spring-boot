/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */

package com.kapeta.spring.config.providers;

import com.kapeta.schemas.entity.BlockDefinition;
import com.kapeta.spring.config.providers.types.*;
import org.springframework.core.env.Environment;

import java.io.IOException;
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


}
