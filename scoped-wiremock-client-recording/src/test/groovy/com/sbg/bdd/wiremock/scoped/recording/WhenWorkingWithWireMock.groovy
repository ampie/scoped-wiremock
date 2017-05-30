package com.sbg.bdd.wiremock.scoped.recording

import com.sbg.bdd.wiremock.scoped.recording.endpointconfig.RemoteEndPointConfigRegistry
import groovy.json.JsonOutput
import org.apache.http.ProtocolVersion
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicStatusLine
import spock.lang.Specification

abstract class WhenWorkingWithWireMock extends Specification {
    public static final int MAX_LEVELS = 10;
    public static final int PRIORITIES_PER_LEVEL = 10;
    static final int EVERYBODY_PRIORITY_DECREMENT = PRIORITIES_PER_LEVEL / 2;

    def initializeWireMockContext() {
        def httpMock = Mock(CloseableHttpClient) {
            execute(_) >> { args ->
                HttpUriRequest request = args[0]
                def body = null;
                if (request.getURI().getPath().endsWith('/Property/all')) {
                    body = JsonOutput.toJson([configs: [
                            [propertyName: 'external.service.a', url: 'http://somehost.com/service/one/endpoint', endpointType: 'REST'],
                            [propertyName: 'external.service.b', url: 'http://somehost.com/service/two/endpoint', endpointType: 'SOAP']

                    ]])
                } else {
                    body = JsonOutput.toJson([propertyName: 'x', url: 'http://somehost.com/resolved/endpoint', endpointType: 'SOAP'])
                }
                return Mock(CloseableHttpResponse) {
                    getEntity() >> new StringEntity(body, ContentType.APPLICATION_JSON)
                    getStatusLine() >> new BasicStatusLine(new ProtocolVersion('http', 2, 0), 200, 'OK')
                }
            }
        }
        return new WireMockContextStub(new RemoteEndPointConfigRegistry(httpMock, 'http://localhost:8080/base'))
    }
}
