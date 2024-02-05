package com.kapeta.spring.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeanHelper {
    private final BeanDefinitionRegistry beanRegistry;
    private final ConfigurableListableBeanFactory beanFactory;
    private final Map<Class,List<Object>> instances = new HashMap<>();

    public BeanHelper(ConfigurableListableBeanFactory beanFactory) {
        this.beanRegistry = (BeanDefinitionRegistry) beanFactory;
        this.beanFactory = beanFactory;
    }

    private boolean isBeanRegistered(Class clz, Object value) {
        if (!instances.containsKey(clz)) {
            instances.put(clz, new ArrayList<>());
        }
        if (instances.get(clz).contains(value)) {
            return true;
        }
        instances.get(clz).add(value);
        return false;
    }

    public <T,U extends T> void registerBean(String beanName, Class<T> clz, U value) {
        if (isBeanRegistered(clz, value)) {
            return;
        }
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(clz);
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        beanDefinition.setAutowireCandidate(true);
        beanDefinition.setInstanceSupplier(() -> value);
        beanRegistry.registerBeanDefinition(beanName, beanDefinition);
    }

    public <T,U extends T> void registerBean( Class<T> clz, U value) {
        if (isBeanRegistered(clz, value)) {
            return;
        }
        beanFactory.registerResolvableDependency(clz, value);
    }
}
