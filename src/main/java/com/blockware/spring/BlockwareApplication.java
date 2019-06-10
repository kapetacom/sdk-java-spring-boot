package com.blockware.spring;

import com.blockware.spring.cluster.BlockwareClusterServiceInitializer;
import org.springframework.boot.SpringApplication;

public class BlockwareApplication {

    /**
     * Use this to start your Spring application - a complete replacement for SpringApplication.run
     * and should be used in the exact same way.
     *
     * @param mainClass
     * @param args
     */
    public static void run(Class<?> mainClass, String[] args) {
        final SpringApplication application = new SpringApplication(mainClass);
        application.addListeners(new BlockwareClusterServiceInitializer());
        application.run(args);
    }
}
