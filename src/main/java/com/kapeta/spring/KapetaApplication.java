package com.kapeta.spring;

import com.kapeta.spring.cluster.KapetaClusterServiceInitializer;
import org.springframework.boot.SpringApplication;

public class KapetaApplication {

    /**
     * Use this to start your Spring application - a complete replacement for SpringApplication.run
     * and should be used in the exact same way.
     *
     * @param mainClass
     * @param args
     */
    public static void run(Class<?> mainClass, String[] args) {
        final SpringApplication application = new SpringApplication(mainClass);
        application.addListeners(new KapetaClusterServiceInitializer());
        application.run(args);
    }
}
