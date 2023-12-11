/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.rest;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@ConditionalOnProperty(name = "kapeta.openapi-redirect", havingValue = "true", matchIfMissing = true)
public class OpenAPIRedirectController {

    @GetMapping("/")
    @Hidden
    public RedirectView redirectToSwaggerDocumentation() {
        return new RedirectView("/swagger-ui/index.html");
    }
}
