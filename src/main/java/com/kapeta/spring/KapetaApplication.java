/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */

package com.kapeta.spring;

import com.kapeta.spring.config.KapetaApplicationInitializer;
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
        application.addListeners(new KapetaApplicationInitializer());
        application.run(args);
    }
}
