package com.blockware.spring.annotation;

import com.blockware.spring.config.BaseConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({BaseConfig.class})
public @interface BlockwareEnableConfig {
}
