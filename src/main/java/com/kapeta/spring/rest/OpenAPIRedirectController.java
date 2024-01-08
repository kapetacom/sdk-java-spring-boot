/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */
package com.kapeta.spring.rest;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@Hidden
@RestController
public class OpenAPIRedirectController {

    @GetMapping("/")
    public RedirectView redirectToSwaggerDocumentation() {
        return new RedirectView("/swagger-ui/index.html");
    }
}
