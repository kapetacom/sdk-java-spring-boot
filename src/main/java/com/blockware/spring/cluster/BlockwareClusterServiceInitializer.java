package com.blockware.spring.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

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


    public static final String CONFIG_SPRING_APPLICATION_NAME = "spring.application.name";
    public static final String CONFIG_BLOCKWARE_SYSTEM_TYPE = "blockware.system.type";
    public static final String CONFIG_BLOCKWARE_SYSTEM_ID = "blockware.system.id";


    public static final String DEFAULT_APPLICATION_NAME = "unknown-application";
    public static final String DEFAULT_SYSTEM_TYPE = "development";
    public static final String DEFAULT_SYSTEM_ID = "default";

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

        final String systemId = getSystemConfiguration(environment,
                BLOCKWARE_SYSTEM_ID,
                CONFIG_BLOCKWARE_SYSTEM_ID,
                DEFAULT_SYSTEM_ID);

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
                configSource = new BlockwareClusterServiceLocal(serviceName, systemId, environment);
                break;

            default:
                throw new RuntimeException("Unknown environment: " + systemType);

        }

        try {

            configSource.load();
            MutablePropertySources propertySources = environment.getPropertySources();
            propertySources.addFirst(configSource);

            applicationContext.getBeanFactory().registerResolvableDependency(BlockwareClusterService.class, configSource);

            log.info("Blockware service initialised with cluster service '{}' for environment '{}' in system '{}'", configSource.getSourceId(), systemType, systemId);
        } catch (ClusterServiceUnavailableException e) {
            log.error(e.getMessage());
            System.exit(1); //Do a hard exit here - we need to cluster service to be available to continue
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise blockware config source for environment: " + systemType + " in system " + systemId, e);
        }
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
