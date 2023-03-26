package com.kapeta.spring.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles initialization of the kapeta cluster service.
 * <p>
 * Needs to happen very early in the boot process which is why it is implemented as an application listener
 * <p>
 * It's for the same reason we've implemented the KapetaApplication.run method.
 */
public class KapetaClusterServiceInitializer implements ApplicationListener<ApplicationPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(KapetaClusterServiceInitializer.class);

    public static final String KAPETA_APPLICATION_NAME = "KAPETA_APPLICATION_NAME";

    public static final String KAPETA_SYSTEM_TYPE = "KAPETA_SYSTEM_TYPE";

    public static final String KAPETA_SYSTEM_ID = "KAPETA_SYSTEM_ID";

    public static final String KAPETA_BLOCK_REF = "KAPETA_BLOCK_REF";

    public static final String KAPETA_INSTANCE_ID = "KAPETA_INSTANCE_ID";

    public static final String KAPETA_BASE_DIR = "KAPETA_BASE_DIR";


    public static final String CONFIG_SPRING_APPLICATION_NAME = "spring.application.name";
    public static final String CONFIG_KAPETA_SYSTEM_TYPE = "kapeta.system.type";
    public static final String CONFIG_KAPETA_SYSTEM_ID = "kapeta.system.id";
    public static final String CONFIG_KAPETA_BLOCK_REF = "kapeta.block.ref";
    public static final String CONFIG_KAPETA_INSTANCE_ID = "kapeta.instance.id";


    public static final String DEFAULT_APPLICATION_NAME = "unknown-application";
    public static final String DEFAULT_SYSTEM_TYPE = "development";
    public static final String DEFAULT_SYSTEM_ID = "";
    public static final String DEFAULT_INSTANCE_ID = "";

    public static final String HEALTH_CHECK_ENDPOINT = "/__kapeta/health";

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        final ConfigurableApplicationContext applicationContext = event.getApplicationContext();

        final ConfigurableEnvironment environment = applicationContext.getEnvironment();

        String blockYMLPath = getBlockYMLPath(environment);
        String blockRefLocal = getBlockRef(blockYMLPath);

        final String serviceName = getSystemConfiguration(environment,
                KAPETA_APPLICATION_NAME,
                CONFIG_SPRING_APPLICATION_NAME,
                DEFAULT_APPLICATION_NAME);

        final String systemType = getSystemConfiguration(environment,
                KAPETA_SYSTEM_TYPE,
                CONFIG_KAPETA_SYSTEM_TYPE,
                DEFAULT_SYSTEM_TYPE).toLowerCase();

        final String blockRef = getSystemConfiguration(environment,
                KAPETA_BLOCK_REF,
                CONFIG_KAPETA_BLOCK_REF,
                blockRefLocal);

        final String systemId = getSystemConfiguration(environment,
                KAPETA_SYSTEM_ID,
                CONFIG_KAPETA_SYSTEM_ID,
                DEFAULT_SYSTEM_ID);

        final String instanceId = getSystemConfiguration(environment,
                KAPETA_INSTANCE_ID,
                CONFIG_KAPETA_INSTANCE_ID,
                DEFAULT_INSTANCE_ID);

        log.info("Starting block instance for block: '{}'", blockRef);

        KapetaClusterService configSource = null;
        switch (systemType) {
            case "staging":
            case "sandbox":

            case "production":
            case "prod":
                throw new RuntimeException("Unimplemented environment support: " + systemType);

            case "development":
            case "dev":
            case "local":
                configSource = new KapetaClusterServiceLocal(blockRef, systemId, instanceId, environment);
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

            applicationContext.getBeanFactory().registerResolvableDependency(KapetaClusterService.class, configSource);

            log.info("Kapeta service initialised with cluster service '{}' for environment '{}' in system '{}'", configSource.getSourceId(), systemType, configSource.getSystemId());
        } catch (ClusterServiceUnavailableException e) {
            log.error(e.getMessage());
            System.exit(1); //Do a hard exit here - we need to cluster service to be available to continue
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise kapeta config source for environment: " + systemType + " in system " + systemId, e);
        }
    }

    private static String getBlockRef(String blockYMLPath) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Map<String, String>> blockDefinition = yaml.load(Files.newInputStream(Path.of(blockYMLPath)));
            String name = blockDefinition.getOrDefault("metadata", new HashMap<>()).getOrDefault("name", "");

            return name + ":local";
        } catch (IOException e) {
            throw new RuntimeException("Failed to read kapeta.yml file", e);
        }
    }

    private String getBlockDir(final ConfigurableEnvironment environment) {
        return environment.getProperty(KAPETA_BASE_DIR,
                Paths.get(".").toAbsolutePath().normalize().toString()
        );
    }

    private String getBlockYMLPath(final ConfigurableEnvironment environment) {

        String blockDir = getBlockDir(environment);

        String blockYMLPath = Paths.get(blockDir, "kapeta.yml").toString();

        if (!new File(blockYMLPath).exists()) {
            throw new RuntimeException("kapeta.yml file not found in path: " + blockDir + ". Ensure that your current working directory contains the kapeta.yml file.");
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
