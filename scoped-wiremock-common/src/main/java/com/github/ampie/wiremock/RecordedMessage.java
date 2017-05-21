package com.github.ampie.wiremock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.tomakehurst.wiremock.http.HttpHeaders;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Base64;
import java.util.Date;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "RecordedMessage",
        propOrder = {"headers","base64Body","date"}
)
public class RecordedMessage {
    @XmlElement(
            nillable = false
    )
    protected String base64Body;
    @XmlElement(
            nillable = false
    )
    protected HttpHeaders headers;
    @XmlElement(
            nillable = false
    )
    private Date date;
    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public String getBase64Body() {
        return base64Body;
    }

    public void setBase64Body(String base64Body) {
        this.base64Body = base64Body;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    @JsonIgnore
    public String getBodyAsString() {
        return new String(Base64.getDecoder().decode(getBodyAsBase64().getBytes()));
    }

    @JsonIgnore
    public String getBodyAsBase64() {
        return getBase64Body();
    }
}
