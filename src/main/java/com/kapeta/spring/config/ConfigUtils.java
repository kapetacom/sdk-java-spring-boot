/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.config;

import org.springframework.core.env.Environment;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigUtils {

    private static final String DOCUMENT = "document";

    public static String getPropertyValue(Environment environment, String key, String defaultValue) {
        return KapetaApplicationInitializer.getSystemConfiguration(environment, key, key.toUpperCase(), defaultValue);
    }

    public static <T> T getValueFromPath(Map<String, Object> data, String path, T defaultValue) {
        var parts = path.split("\\.");
        var current = data;
        for (int i = 0; i < parts.length; i++) {
            var part = parts[i];
            if (i == parts.length - 1) {
                return (T) current.getOrDefault(part, defaultValue);
            } else {
                if (current.containsKey(part)) {
                    var value = current.get(part);
                    if (value instanceof Map) {
                        current = (Map<String, Object>) value;
                    } else {
                        return defaultValue;
                    }
                } else {
                    return defaultValue;
                }
            }
        }

        return defaultValue;
    }

    public static Properties getPropertiesFromYAML(File yamlFile, Environment environment) {
        var out = new Properties();

        if (!yamlFile.exists()) {
            return out;
        }

        try (InputStream stream = new FileInputStream(yamlFile)) {
            applyYAMLFromStream(environment, stream, out);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load YAML configuration from: " + yamlFile, e);
        }

        return out;
    }



    private static void applyYAMLFromStream(Environment environment, InputStream stream, Properties properties) throws IOException {
        final Yaml yaml = new Yaml();

        try (Reader reader = new UnicodeReader(stream)) {
            for (Object object : yaml.loadAll(reader)) {
                if (object == null) {
                    continue;
                }

                applyFlattenedObjectToProperties(environment, object, properties);
            }
        }
    }

    public static void applyFlattenedObjectToProperties(Environment environment, Object object, Properties properties) {
        final Map<String, Object> stringObjectMap = asFlatMap(environment, object);
        for (Map.Entry<String, Object> entry : stringObjectMap.entrySet()) {
            properties.setProperty(entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    /**
     * Takes a nested data structure such as YAML and converts to a simple key/value
     */
    private static Map<String, Object> asFlatMap(Environment environment, Object object) {
        return asFlatMap(environment, object, "");
    }

    private static Map<String, Object> asFlatMap(Environment environment, Object object, String parentPath) {
        // YAML can have numbers as keys
        Map<String, Object> result = new LinkedHashMap<>();
        if (!(object instanceof Map)) {
            // A document can be a text literal
            result.put(DOCUMENT, object);
            return result;
        }

        Map<Object, Object> map = (Map<Object, Object>) object;
        map.forEach((key, value) -> {

            String path = key.toString();
            if (!parentPath.isEmpty()) {
                path = parentPath + "." + key;
            }

            if (value instanceof Map) {
                result.putAll(asFlatMap(environment, value, path));
                return;
            }

            if (value instanceof String) {
                value = environment.resolveRequiredPlaceholders((String) value);
            }

            if (key instanceof CharSequence) {
                result.put(path, value);
            } else {
                // It has to be a map key in this case
                result.put(parentPath + "[" + key.toString() + "]", value);
            }
        });
        return result;
    }
}
