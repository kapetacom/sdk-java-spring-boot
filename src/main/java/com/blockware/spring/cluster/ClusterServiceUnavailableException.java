package com.blockware.spring.cluster;

import java.io.IOException;

public class ClusterServiceUnavailableException extends IOException {

    public ClusterServiceUnavailableException(String message) {
        super(message);
    }
}
