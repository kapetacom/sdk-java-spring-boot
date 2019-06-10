package com.blockware.spring.annotation;

import com.blockware.spring.cluster.BlockwareDefaultConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({BlockwareDefaultConfig.class})
public @interface BlockwareEnableDefaultConfig {
}
