/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.annotation;

import com.kapeta.spring.config.KapetaTestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.*;

/**
 * Annotation to enable kapeta test context
 * <p>
 * This will provide the basics needed by the Kapeta SDK
 * when running tests.
 * <p>
 * To provide adjustments to the configuration, you can
 * add beans of type {@link com.kapeta.spring.config.providers.TestConfigProvider.TestConfigurationAdjuster}
 * which will be called to adjust the configuration.
 *
 * This annotation should typically be applied to a @Configuration-annotated class in your test.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({KapetaTestConfig.class})
public @interface KapetaTestContext {
}
