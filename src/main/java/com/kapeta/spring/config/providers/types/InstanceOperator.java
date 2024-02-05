/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.config.providers.types;

import java.util.HashMap;
import java.util.Map;

public class InstanceOperator<Options, Credentials> {
    private String hostname;
    private Map<String, InstanceOperatorPort> ports = new HashMap<>();
    private String path;
    private String query;
    private String hash;
    private Credentials credentials;
    private Options options;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Map<String, InstanceOperatorPort> getPorts() {
        return ports;
    }

    public void setPorts(Map<String, InstanceOperatorPort> ports) {
        this.ports = ports;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }
}
