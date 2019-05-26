package com.blockware.spring;

import com.blockware.spring.config.BlockwareConfigSourceLoader;
import org.springframework.boot.SpringApplication;

public class Blockware {

    public static void run(Class<?> mainClass, String[] args) {
        final SpringApplication application = new SpringApplication(mainClass);
        application.addListeners(new BlockwareConfigSourceLoader());

        application.run(args);
    }
}
