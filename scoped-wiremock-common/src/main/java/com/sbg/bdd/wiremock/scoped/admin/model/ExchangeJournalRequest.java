package com.sbg.bdd.wiremock.scoped.admin.model;

import com.github.tomakehurst.wiremock.matching.RequestPattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "ExchangeJournalRequest",
        propOrder = {"mode", "resourceRoot", "path", "requestPattern"}
)
public class ExchangeJournalRequest {
    @XmlElement(
            nillable = false
    )
    private JournalMode mode;
    @XmlElement(
            nillable = false
    )
    private String resourceRoot;
    @XmlElement(
            nillable = false
    )
    private String path;
    @XmlElement(
            nillable = false
    )
    private ExtendedRequestPattern requestPattern;
    private Integer priority;

    public ExchangeJournalRequest() {
    }

    public ExchangeJournalRequest(JournalMode mode, String resourceRoot, String path, ExtendedRequestPattern requestPattern) {
        this.mode = mode;
        this.resourceRoot = resourceRoot;
        this.path = path;
        this.requestPattern = requestPattern;
    }

    public ExchangeJournalRequest(JournalMode mode, String rootName, String path, ExtendedRequestPattern requestPattern, int priority) {
        this(mode,rootName,path,requestPattern);
        this.priority = priority;
    }

    public JournalMode getMode() {
        return mode;
    }

    public String getResourceRoot() {
        return resourceRoot;
    }

    public String getPath() {
        return path;
    }

    public ExtendedRequestPattern getRequestPattern() {
        return requestPattern;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
