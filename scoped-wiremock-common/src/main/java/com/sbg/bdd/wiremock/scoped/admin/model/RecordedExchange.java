package com.sbg.bdd.wiremock.scoped.admin.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sbg.bdd.wiremock.scoped.common.ParentPath;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "RecordedExchange",
        propOrder = {"request","response","scopePath","step","duration"}
)
//TODO extend ServeEvent. The problem is the LoggedResponse field that can't be set
public class RecordedExchange {
    @XmlElement(
            nillable = false
    )
    private String scopePath;
    @XmlElement(
            nillable = true
    )
    private String step;
    @XmlElement(
            nillable = false
    )
    private int sequenceNumber = 0;
    @XmlElement(
            nillable = false
    )
    private int threadContextId = 0;
    @XmlElement(
            nillable = false
    )

    private RecordedRequest request;
    @XmlElement(
            nillable = false
    )
    private RecordedResponse response;

    @XmlElement(
            nillable = false
    )
    private long duration;
    @XmlElement(
            nillable = false
    )
    private List<RecordedExchange> nestedExchanges = new ArrayList<>();
    @JsonIgnore
    private boolean rootExchange;
    public RecordedExchange() {
    }

    public RecordedExchange(RecordedRequest request) {
        this.request = request;
    }

    public RecordedExchange(RecordedRequest recordedRequest, String scopePath, String step) {
        this(recordedRequest);
        this.scopePath = scopePath;
        this.step = step;
    }


    public RecordedExchange(RecordedExchange recordedExchange) {
        this.request=recordedExchange.getRequest();
        this.response=recordedExchange.getResponse();
        this.scopePath=recordedExchange.getScopePath();
        this.duration=recordedExchange.getDuration();
        this.step = recordedExchange.getStep();
        this.threadContextId=recordedExchange.getThreadContextId();
        this.sequenceNumber=recordedExchange.getSequenceNumber();
    }

    public RecordedResponse getResponse() {
        return response;
    }

    public void setResponse(RecordedResponse response) {
        this.response = response;
        duration=response.getDate().getTime()-request.getDate().getTime();

    }


    public RecordedRequest getRequest() {
        return request;
    }

    public void setRequest(RecordedRequest request) {
        this.request = request;
    }

    public String getScopePath() {
        return scopePath;
    }

    public void setScopePath(String scopePath) {
        this.scopePath = scopePath;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public synchronized void recordNestedExchange(RecordedExchange exchange) {
        nestedExchanges.add(exchange);
    }

    public void recordResponse(RecordedResponse response) {
        this.response=response;
        setDuration(response.getDate().getTime() - request.getDate().getTime());
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getThreadContextId() {
        return threadContextId;
    }

    public void setThreadContextId(int threadContextId) {
        this.threadContextId = threadContextId;
    }

    public synchronized  List<RecordedExchange> getNestedExchanges() {
        return new ArrayList<>(nestedExchanges);
    }

    public boolean isRootExchange() {
        return rootExchange;
    }

    public void setRootExchange(boolean rootExchange) {
        this.rootExchange = rootExchange;
    }
}
