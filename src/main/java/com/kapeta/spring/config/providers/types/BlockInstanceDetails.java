/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.config.providers.types;

import com.kapeta.schemas.entity.Connection;

import java.util.ArrayList;
import java.util.List;

public class BlockInstanceDetails<BlockType> {
    private String instanceId;

    private BlockType block;

    private List<Connection> connections = new ArrayList<>();

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public BlockType getBlock() {
        return block;
    }

    public void setBlock(BlockType block) {
        this.block = block;
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public void setConnections(List<Connection> connections) {
        this.connections = connections;
    }
}
