package com.blockware.spring.cluster;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;

/**
 * Handles initialization of the blockware cluster service.
 *
 * Needs to happen very early in the boot process which is why it is implemented as an application listener
 *
 * It's for the same reason we've implemented the BlockwareApplication.run method.
 */
public class BlockwareClusterServiceInitializer implements ApplicationListener<ApplicationPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(BlockwareClusterServiceInitializer.class);

    public static final String BLOCKWARE_APPLICATION_NAME = "BLOCKWARE_APPLICATION_NAME";

    public static final String BLOCKWARE_SYSTEM_TYPE = "BLOCKWARE_SYSTEM_TYPE";

    public static final String BLOCKWARE_SYSTEM_ID = "BLOCKWARE_SYSTEM_ID";

    public static final String BLOCKWARE_BLOCK_REF = "BLOCKWARE_BLOCK_REF";

    public static final String BLOCKWARE_INSTANCE_ID = "BLOCKWARE_INSTANCE_ID";

    public static final String BLOCKWARE_BASE_DIR = "BLOCKWARE_BASE_DIR";


    public static final String CONFIG_SPRING_APPLICATION_NAME = "spring.application.name";
    public static final String CONFIG_BLOCKWARE_SYSTEM_TYPE = "blockware.system.type";
    public static final String CONFIG_BLOCKWARE_SYSTEM_ID = "blockware.system.id";
    public static final String CONFIG_BLOCKWARE_BLOCK_REF = "blockware.block.ref";
    public static final String CONFIG_BLOCKWARE_INSTANCE_ID = "blockware.instance.id";


    public static final String DEFAULT_APPLICATION_NAME = "unknown-application";
    public static final String DEFAULT_SYSTEM_TYPE = "development";
    public static final String DEFAULT_SYSTEM_ID = "";
    public static final String DEFAULT_INSTANCE_ID = "";

    public static final String HEALTH_CHECK_ENDPOINT = "/__blockware/health";

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        final ConfigurableApplicationContext applicationContext = event.getApplicationContext();

        final ConfigurableEnvironment environment = applicationContext.getEnvironment();

        final String serviceName = getSystemConfiguration(environment,
                BLOCKWARE_APPLICATION_NAME,
                CONFIG_SPRING_APPLICATION_NAME,
                DEFAULT_APPLICATION_NAME);

        final String systemType = getSystemConfiguration(environment,
                BLOCKWARE_SYSTEM_TYPE,
                CONFIG_BLOCKWARE_SYSTEM_TYPE,
                DEFAULT_SYSTEM_TYPE).toLowerCase();

        final String blockRef = getSystemConfiguration(environment,
                BLOCKWARE_BLOCK_REF,
                CONFIG_BLOCKWARE_BLOCK_REF,
                "file://" + getBlockYMLPath(environment));

        final String systemId = getSystemConfiguration(environment,
                BLOCKWARE_SYSTEM_ID,
                CONFIG_BLOCKWARE_SYSTEM_ID,
                DEFAULT_SYSTEM_ID);

        final String instanceId = getSystemConfiguration(environment,
                BLOCKWARE_INSTANCE_ID,
                CONFIG_BLOCKWARE_INSTANCE_ID,
                DEFAULT_INSTANCE_ID);

        log.info("Starting block instance for block: '{}'", blockRef);

        BlockwareClusterService configSource = null;
        switch (systemType) {
            case "staging":
            case "sandbox":

            case "production":
            case "prod":
                throw new RuntimeException("Unimplemented environment support: " + systemType);

            case "development":
            case "dev":
            case "local":
                configSource = new BlockwareClusterServiceLocal(blockRef, systemId, instanceId, environment);
                break;

            default:
                throw new RuntimeException("Unknown environment: " + systemType);

        }

        try {
            configSource.load();

            //Tell the cluster service about this instance
            configSource.registerInstance(HEALTH_CHECK_ENDPOINT);
            Runtime.getRuntime().addShutdownHook(new Thread(configSource::instanceStopped));

            MutablePropertySources propertySources = environment.getPropertySources();
            propertySources.addFirst(configSource);

            applicationContext.getBeanFactory().registerResolvableDependency(BlockwareClusterService.class, configSource);

            log.info("Blockware service initialised with cluster service '{}' for environment '{}' in system '{}'", configSource.getSourceId(), systemType, configSource.getSystemId());
        } catch (ClusterServiceUnavailableException e) {
            log.error(e.getMessage());
            System.exit(1); //Do a hard exit here - we need to cluster service to be available to continue
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise blockware config source for environment: " + systemType + " in system " + systemId, e);
        }
    }

    private String getBlockDir(final ConfigurableEnvironment environment) {
        return environment.getProperty(BLOCKWARE_BASE_DIR,
            Paths.get(".").toAbsolutePath().normalize().toString()
        );
    }

    private String getBlockYMLPath(final ConfigurableEnvironment environment) {

        String blockDir = getBlockDir(environment);

        String blockYMLPath = Paths.get(blockDir, "block.yml").toString();

        if (!new File(blockYMLPath).exists()) {
            //Try again with .yaml
            blockYMLPath = Paths.get(blockDir, "block.yaml").toString();
        }

        if (!new File(blockYMLPath).exists()) {
            throw new RuntimeException("block.yml or block.yaml file not found in path: " + blockDir + ". Ensure that your current working directory contains the block.yml file.");
        }

        return blockYMLPath;
    }

    static String getSystemConfiguration(final Environment environment, String environmentKey, String configKey, String defaultValue) {
        return getSystemConfiguration(environment, environmentKey, environmentKey, configKey, defaultValue);
    }

    private static String getSystemConfiguration(final Environment environment, String propertyKey, String environmentKey, String configKey, String defaultValue) {
        String value = System.getProperty(propertyKey);
        if (!StringUtils.isEmpty(value)) {
            return value;
        }

        value = System.getenv(environmentKey);

        if (!StringUtils.isEmpty(value)) {
            return value;
        }

        return environment.getProperty(configKey, defaultValue);
    }
}
