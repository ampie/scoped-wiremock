package com.sbg.bdd.wiremock.scoped.admin.model;

import com.github.tomakehurst.wiremock.common.Json;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "CorrelationState",
        propOrder = {"correlationPath", "serviceInvocationCounts","currentStep","payload"}
)
public class CorrelationState {
    @XmlElement(
            nillable = true
    )
    private String correlationPath;
    @XmlElement(
            nillable = true
    )
    private Map<String, Integer> serviceInvocationCounts = new HashMap<>();
    @XmlElement(
            nillable = true
    )
    private String currentStep;
    @XmlElement(
            nillable = true
    )
    private Map<String, Object> payload= new HashMap<>();

    public CorrelationState() {
    }

    public CorrelationState(String correlationPath) {
        this.correlationPath = correlationPath;
    }
    public CorrelationState(String correlationPath,String currentStep) {
        this.correlationPath = correlationPath;
        this.currentStep = currentStep;
    }

    public CorrelationState(String correlationPath, Map<String, Object> map) {
        this(correlationPath);
        payload=map;
    }

    public CorrelationState(String scopePath, String stepName, Map<String, Object> payload) {
        this(scopePath,stepName);
        this.payload=payload;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public String getCorrelationPath() {
        return correlationPath;
    }

    public void setCorrelationPath(String correlationPath) {
        this.correlationPath = correlationPath;
    }

    public Map<String, Integer> getServiceInvocationCounts() {
        return serviceInvocationCounts;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

}