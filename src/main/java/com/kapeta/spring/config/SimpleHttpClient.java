package com.kapeta.spring.config;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SimpleHttpClient {
    public static final String KAPETA_ENVIRONMENT_TYPE = "KAPETA_ENVIRONMENT_TYPE";

    public static final String HEADER_KAPETA_ENVIRONMENT = "X-Kapeta-Environment";

    public static final String HEADER_KAPETA_BLOCK = "X-Kapeta-Block";

    public static final String HEADER_KAPETA_SYSTEM = "X-Kapeta-System";

    public static final String HEADER_KAPETA_INSTANCE = "X-Kapeta-Instance";

    private final String blockRef;

    private String systemId;

    private String instanceId;

    public SimpleHttpClient(String blockRef, String systemId, String instanceId) {
        this.blockRef = blockRef;
        this.systemId = systemId;
        this.instanceId = instanceId;
    }

    public String getBlockRef() {
        return blockRef;
    }

    public String getSystemId() {
        return systemId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Helper method for sending a GET request to a URL which will include the proper headers etc.
     * <p>
     * Returns the response body as a string
     */
    public String sendGET(final String url) throws IOException {

        try (InputStream stream = sendRequestStream(url, "GET")) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }

    public String sendDELETE(final String url) throws IOException {

        try (InputStream stream = sendRequestStream(url, "DELETE")) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }

    public String sendPUT(final String url, byte[] body) throws IOException {

        try (InputStream stream = sendRequestStream(url, "PUT", new ByteArrayInputStream(body))) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }

    public InputStream sendRequestStream(final String url, String method) throws IOException {
        return sendRequestStream(url, method, null);
    }

    /**
     * Helper method for sending a GET request to a URL which will include the proper headers etc.
     * <p>
     * Returns the response body as a stream
     */
    private InputStream sendRequestStream(final String urlString, String method, InputStream body) throws IOException {
        final URL url = new URL(urlString);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (body != null) {
            connection.setDoOutput(true);
        }

        connection.setRequestMethod(method);

        var environment = "process";
        if (System.getenv().containsKey(KAPETA_ENVIRONMENT_TYPE)) {
            environment = System.getenv(KAPETA_ENVIRONMENT_TYPE);
        }
        connection.addRequestProperty(HEADER_KAPETA_ENVIRONMENT, environment);
        connection.addRequestProperty(HEADER_KAPETA_BLOCK, blockRef);
        connection.addRequestProperty(HEADER_KAPETA_SYSTEM, systemId);
        connection.addRequestProperty(HEADER_KAPETA_INSTANCE, instanceId);

        if (body != null) {
            IOUtils.copy(body, connection.getOutputStream());
        }

        if (connection.getResponseCode() > 399) {
            var response = IOUtils.toString(connection.getErrorStream(), StandardCharsets.UTF_8);
            throw new IOException("Failed to send request: " + connection.getResponseCode() + " " + response);
        }

        return connection.getInputStream();
    }
}
