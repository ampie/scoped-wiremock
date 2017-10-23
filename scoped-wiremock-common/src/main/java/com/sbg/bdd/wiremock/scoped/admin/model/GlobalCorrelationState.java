package com.sbg.bdd.wiremock.scoped.admin.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.net.MalformedURLException;
import java.net.URL;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "GlobalCorrelationState"
)
public class GlobalCorrelationState extends CorrelationState{
    @XmlElement(
            nillable = true
    )
    private String runName;
    @XmlElement(
            nillable = true
    )
    private Integer sequenceNumber;
    @XmlElement(
            nillable = true
    )
    private String wireMockPublicUrl;
    @XmlElement(
            nillable = true
    )
    private String urlOfServiceUnderTest;
    @XmlElement(
            nillable = true
    )
    private String integrationScope;
    @XmlElement(
            nillable = true
    )
    private JournalMode globalJournaMode;

    public GlobalCorrelationState() {
    }

    public GlobalCorrelationState(String runName, URL wireMockPublicUrl, URL urlOfServiceUnderTest, String integrationScope) {
        this.runName = runName;
        this.wireMockPublicUrl = wireMockPublicUrl.toExternalForm();
        this.urlOfServiceUnderTest = urlOfServiceUnderTest==null?null:urlOfServiceUnderTest.toExternalForm();
        this.integrationScope = integrationScope;
    }

    public GlobalCorrelationState(String runName, URL wireMockPublicUrl, int sequenceNumber) {
        this.runName = runName;
        this.wireMockPublicUrl = wireMockPublicUrl.toExternalForm();
        this.sequenceNumber = sequenceNumber;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getRunName() {
        return runName;
    }

    public URL getWireMockPublicUrl() {
        return toUrl(this.wireMockPublicUrl);
    }

    private URL toUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public URL getUrlOfServiceUnderTest() {
        return toUrl(urlOfServiceUnderTest);
    }

    public String getIntegrationScope() {
        return integrationScope;
    }

    public JournalMode getGlobalJournaMode() {
        return globalJournaMode;
    }

    public void setGlobalJournaMode(JournalMode globalJournaMode) {
        this.globalJournaMode = globalJournaMode;
    }
}
