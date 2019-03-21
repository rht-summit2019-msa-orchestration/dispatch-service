package com.acme.ride.dispatch.tracing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="jaeger")
public class JaegerTracerProperties {

    private String serviceName;

    private String samplerType;

    private Number samplerParam;

    private String samplerManagerHostPort;

    private boolean reporterLogSpans;

    private String agentHost;

    private Integer agentPort;

    private Integer reporterFlushInterval;

    private Integer reporterMaxQueueSize;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getSamplerType() {
        return samplerType;
    }

    public void setSamplerType(String samplerType) {
        this.samplerType = samplerType;
    }

    public Number getSamplerParam() {
        return samplerParam;
    }

    public void setSamplerParam(Number samplerParam) {
        this.samplerParam = samplerParam;
    }

    public String getSamplerManagerHostPort() {
        return samplerManagerHostPort;
    }

    public void setSamplerManagerHostPort(String samplerManagerHostPort) {
        this.samplerManagerHostPort = samplerManagerHostPort;
    }

    public boolean isReporterLogSpans() {
        return reporterLogSpans;
    }

    public void setReporterLogSpans(boolean reporterLogSpans) {
        this.reporterLogSpans = reporterLogSpans;
    }

    public String getAgentHost() {
        return agentHost;
    }

    public void setAgentHost(String agentHost) {
        this.agentHost = agentHost;
    }

    public Integer getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(Integer agentPort) {
        this.agentPort = agentPort;
    }

    public Integer getReporterFlushInterval() {
        return reporterFlushInterval;
    }

    public void setReporterFlushInterval(Integer reporterFlushInterval) {
        this.reporterFlushInterval = reporterFlushInterval;
    }

    public Integer getReporterMaxQueueSize() {
        return reporterMaxQueueSize;
    }

    public void setReporterMaxQueueSize(Integer reporterMaxQueueSize) {
        this.reporterMaxQueueSize = reporterMaxQueueSize;
    }
}
