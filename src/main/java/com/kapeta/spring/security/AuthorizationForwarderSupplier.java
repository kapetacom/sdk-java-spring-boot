/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.security;

import java.util.function.Supplier;

public interface AuthorizationForwarderSupplier extends Supplier<AuthorizationForwarder> {

}
