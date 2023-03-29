package com.kapeta.spring.annotation;

import com.kapeta.spring.cluster.KapetaDefaultConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({KapetaDefaultConfig.class})
public @interface KapetaEnableDefaultConfig {
}