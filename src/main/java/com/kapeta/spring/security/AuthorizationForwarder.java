package com.kapeta.spring.security;

public interface AuthorizationForwarder {

    default String getAuthorizationHeader() {
        return "Authorization";
    }

    String getAuthorizationValue();
}
