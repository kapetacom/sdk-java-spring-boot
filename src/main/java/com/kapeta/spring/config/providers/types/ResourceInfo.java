/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.config.providers.types;

import java.util.HashMap;
import java.util.Map;

public class ResourceInfo {

    private String host;

    private String port;

    private String type;

    private String protocol;

    private String resource;

    private Map<String, Object> options = new HashMap<>();

    private Map<String, String> credentials = new HashMap<>();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, String> credentials) {
        this.credentials = credentials;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public ResourceInfo withHost(String host) {
        this.host = host;
        return this;
    }

    public ResourceInfo withPort(String port) {
        this.port = port;
        return this;
    }

    public ResourceInfo withType(String type) {
        this.type = type;
        return this;
    }

    public ResourceInfo withProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public ResourceInfo withOptions(Map<String, Object> options) {
        this.options = options;
        return this;
    }

    public ResourceInfo withOption(String key, Object value) {
        this.options.put(key, value);
        return this;
    }

    public ResourceInfo withCredentials(Map<String, String> credentials) {
        this.credentials = credentials;
        return this;
    }

    public ResourceInfo withCredential(String key, String value) {
        this.credentials.put(key, value);
        return this;
    }


    public ResourceInfo withResource(String resource) {
        this.resource = resource;
        return this;
    }

}
