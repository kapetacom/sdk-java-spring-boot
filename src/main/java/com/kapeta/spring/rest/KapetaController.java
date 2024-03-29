/*
 * Copyright 2023 Kapeta Inc.
 * SPDX-License-Identifier: MIT
 */

package com.kapeta.spring.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RequestMapping(".kapeta")
@RestController
@Hidden
public class KapetaController {

    private final ObjectMapper objectMapper;

    public KapetaController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RequestMapping("health")
    public void health(HttpServletResponse response) throws IOException {

        response.setHeader("Content-Type", "application/json");
        response.setStatus(200);

        objectMapper.writeValue(response.getWriter(), new Health());
        response.flushBuffer();
    }

    public static class Health {
        boolean ok = true;

        public boolean isOk() {
            return ok;
        }

        public void setOk(boolean ok) {
            this.ok = ok;
        }
    }
}
