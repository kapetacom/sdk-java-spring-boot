/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */

package com.kapeta.spring.config;

import java.io.IOException;

public class ClusterServiceUnavailableException extends IOException {

    public ClusterServiceUnavailableException(String message) {
        super(message);
    }
}
