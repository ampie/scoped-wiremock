package com.sbg.bdd.wiremock.scoped.admin.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.HashMap;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "CorrelationState",
        propOrder = {"parentCorrelationPath", "name", "payload"}
)
public class InitialScopeState {
    @XmlElement(
            nillable = true
    )
    private String parentCorrelationPath;
    @XmlElement(
            nillable = true
    )
    private String name;
    @XmlElement(
            nillable = true
    )
    private Map<String, Object> payload=new HashMap<>();

    public InitialScopeState() {
    }

    public InitialScopeState(String parentCorrelationPath, String name, Map<String, Object> payload) {
        this.parentCorrelationPath = parentCorrelationPath;
        this.name = name;
        this.payload = payload;
    }

    public String getParentCorrelationPath() {
        return parentCorrelationPath;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
