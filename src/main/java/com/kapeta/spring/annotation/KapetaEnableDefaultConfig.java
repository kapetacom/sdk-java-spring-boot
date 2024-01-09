/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */

package com.kapeta.spring.annotation;

import com.kapeta.spring.config.KapetaDefaultConfig;
import com.kapeta.spring.config.KapetaRestControllerConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({KapetaDefaultConfig.class, KapetaRestControllerConfig.class})
public @interface KapetaEnableDefaultConfig {
}
