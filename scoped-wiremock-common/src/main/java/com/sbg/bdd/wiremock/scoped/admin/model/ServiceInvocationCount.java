package com.sbg.bdd.wiremock.scoped.admin.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "ServiceInvocationCount",
        propOrder = {"threadContextId", "endpointIdentifier", "count"}
)
public class ServiceInvocationCount implements Comparable<ServiceInvocationCount> {
    @XmlElement(
    )
    private int threadContextId;
    @XmlElement(
    )
    private String endpointIdentifier;
    @XmlElement(
    )
    private int count;

    public ServiceInvocationCount() {
    }

    public ServiceInvocationCount(int threadContextId, String endpointIdentifier, int count) {
        this.threadContextId = threadContextId;
        this.endpointIdentifier = endpointIdentifier;
        this.count = count;
    }

    public ServiceInvocationCount(String string) {
        String[] split = string.split("\\|");
        this.threadContextId = Integer.parseInt(split[0]);
        this.endpointIdentifier = split[1];
        this.count = Integer.parseInt(split[2]);
    }
    @JsonIgnore
    public String getKey() {
        return keyOf(threadContextId, endpointIdentifier);
    }

    public static String keyOf(int threadContextId, String endpointIdentifier) {
        return threadContextId + endpointIdentifier;
    }

    public int getThreadContextId() {
        return threadContextId;
    }

    public String getEndpointIdentifier() {
        return endpointIdentifier;
    }

    public int getCount() {
        return count;
    }

    public int next() {
        return ++count;
    }

    public String toString() {
        return threadContextId + "|" + endpointIdentifier + "|" + count;
    }

    @Override
    public int compareTo(ServiceInvocationCount serviceInvocationCount) {
        if (serviceInvocationCount.threadContextId == threadContextId) {
            return endpointIdentifier.compareTo(serviceInvocationCount.endpointIdentifier);
        }
        return threadContextId - serviceInvocationCount.threadContextId;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
