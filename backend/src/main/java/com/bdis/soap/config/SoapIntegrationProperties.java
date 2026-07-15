package com.bdis.soap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bdis.soap")
public class SoapIntegrationProperties {

    private String mode = "disabled";
    private String campusEndpoint = "http://localhost:8080/api/services/campus-growth";
    private long connectTimeoutMs = 3000;
    private long readTimeoutMs = 5000;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getCampusEndpoint() {
        return campusEndpoint;
    }

    public void setCampusEndpoint(String campusEndpoint) {
        this.campusEndpoint = campusEndpoint;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public boolean isMockMode() {
        return "mock".equalsIgnoreCase(mode);
    }
}
