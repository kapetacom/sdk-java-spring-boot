/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.security;

public interface AuthorizationForwarder {

    default String getAuthorizationHeader() {
        return "Authorization";
    }

    String getAuthorizationValue();
}
