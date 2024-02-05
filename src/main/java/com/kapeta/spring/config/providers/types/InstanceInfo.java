/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.config.providers.types;

public class InstanceInfo {

    private String pid;

    private String health;

    public InstanceInfo(String pid, String health) {
        this.pid = pid;
        this.health = health;
    }

    public String getPid() {
        return pid;
    }

    public String getHealth() {
        return health;
    }
}
