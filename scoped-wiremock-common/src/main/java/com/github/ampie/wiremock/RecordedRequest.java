package com.github.ampie.wiremock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.tomakehurst.wiremock.common.Urls;
import com.github.tomakehurst.wiremock.http.*;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.common.Urls.splitQuery;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "RecordedRequest",
        propOrder = {"method", "requestedUrl", "sequenceNumber"}
)
public class RecordedRequest extends RecordedMessage implements Request {
    @XmlElement(
            nillable = false
    )
    private RequestMethod method;
    @XmlElement(
            nillable = false
    )
    //TODO rename to PATH
    private String requestedUrl;
    @XmlElement(
            nillable = false
    )
    private int sequenceNumber = 0;
    @XmlElement
    private Map<String, Cookie> cookies = new HashMap<>();
    @JsonIgnore
    private Map<String, QueryParameter> queryParams = new HashMap<>();
    @XmlElement
    private String clientIp;
    @XmlElement
    private String absoluteUrl;
    @XmlElement
    private String url;
    @XmlElement
    private byte[] body=new byte[0];
    @XmlElement
    private boolean browserProxyRequest=false;

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getAbsoluteUrl() {
        return absoluteUrl;
    }

    public RequestMethod getMethod() {
        return method;
    }

    @Override
    public String getClientIp() {
        return clientIp;
    }

    @Override
    public String getHeader(String key) {
        HttpHeader header = header(key);
        if (header == null) {
            return null;
        }
        return header.firstValue();
    }

    @Override
    public HttpHeader header(String key) {
        return getHeaders().getHeader(key);
    }

    @Override
    public ContentTypeHeader contentTypeHeader() {
        return getHeaders().getContentTypeHeader();
    }

    @Override
    public boolean containsHeader(String key) {
        return getHeaders().getHeader(key) != null;
    }

    @Override
    public Set<String> getAllHeaderKeys() {
        return getHeaders().keys();
    }

    public void setCookies(Map<String, Cookie> cookies) {
        this.cookies.clear();
        if(cookies!=null){
            this.cookies.putAll(cookies);
        }
    }

    public void setQueryParams(Map<String, QueryParameter> queryParams) {
        this.queryParams.clear();
        if(queryParams!=null){
            this.queryParams.putAll(queryParams);
        }
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return cookies;
    }

    @Override
    public QueryParameter queryParameter(String key) {
        return queryParams.get(key);
    }

    @JsonIgnore
    @Override
    public byte[] getBody() {
        return getBodyAsString().getBytes();
    }


    @JsonIgnore
    @Override
    public boolean isBrowserProxyRequest() {
        return false;
    }

    public void setMethod(RequestMethod method) {
        this.method = method;
    }

    public String getRequestedUrl() {
        return requestedUrl;
    }

    public void setRequestedUrl(String requestedUrl) {
        URI uri = URI.create(requestedUrl);
        this.queryParams = Urls.splitQuery(uri);
        this.url=requestedUrl;
        this.requestedUrl=uri.getPath();
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setAbsoluteUrl(String absoluteUrl) {
        this.absoluteUrl = absoluteUrl;
    }
    @JsonIgnore
    public Map<String, QueryParameter> getQueryParameters() {
        if(queryParams==null){
            queryParams= Urls.splitQuery(URI.create(getAbsoluteUrl()));
        }
        return queryParams;
    }
}
