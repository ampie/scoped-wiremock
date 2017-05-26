package com.sbg.bdd.wiremock.scoped.admin.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sbg.bdd.wiremock.scoped.common.ExecutionPathExtractor;

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
    }

    public RecordedResponse getResponse() {
        return response;
    }

    public void setResponse(RecordedResponse response) {
        this.response = response;
        duration=response.getDate().getTime()-request.getDate().getTime();

    }
    @JsonIgnore
    public String getExecutionScopePath(){
        String scopePath = this.scopePath;
        return ExecutionPathExtractor.executionScopePathFrom(scopePath);

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

    @JsonIgnore
    public RecordedExchange getInnerMostActiveExchange() {
        if(nestedExchanges.isEmpty()){
            return this;
        }else {
            RecordedExchange exchange = nestedExchanges.get(nestedExchanges.size() - 1);
            if(exchange.getResponse()==null){
                return exchange.getInnerMostActiveExchange();
            }else{
                return this;
            }
        }
    }

    public void recordNestedExchange(RecordedExchange exchange) {
        nestedExchanges.add(exchange);
    }

    public void recordResponse(RecordedResponse response) {
        this.response=response;
    }

    public List<RecordedExchange> getNestedExchanges() {
        return nestedExchanges;
    }

    public boolean isRootExchange() {
        return rootExchange;
    }

    public void setRootExchange(boolean rootExchange) {
        this.rootExchange = rootExchange;
    }
}
