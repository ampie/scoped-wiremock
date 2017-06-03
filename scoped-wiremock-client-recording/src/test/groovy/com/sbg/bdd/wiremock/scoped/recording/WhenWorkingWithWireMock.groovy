package com.sbg.bdd.wiremock.scoped.recording

import com.sbg.bdd.wiremock.scoped.recording.endpointconfig.RemoteEndPointConfigRegistry
import groovy.json.JsonOutput
import okhttp3.Call
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
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
        def httpMock = Mock(OkHttpClient) {
            newCall(_) >> { args ->
                Request request = args[0]
                def body = null;
                if (request.url().toString().endsWith('/Property/all')) {
                    body = JsonOutput.toJson([configs: [
                            [propertyName: 'external.service.a', url: 'http://somehost.com/service/one/endpoint', endpointType: 'REST'],
                            [propertyName: 'external.service.b', url: 'http://somehost.com/service/two/endpoint', endpointType: 'SOAP']

                    ]])
                } else {
                    body = JsonOutput.toJson([propertyName: 'x', url: 'http://somehost.com/resolved/endpoint', endpointType: 'SOAP'])
                }
                return Mock(Call) {
                    execute() >>{
                        new Response.Builder().request(request).body(ResponseBody.create(MediaType.parse("application/json"), body)).code(200).protocol(Protocol.HTTP_2).message("OK").build();
                    }
                }
            }
        }
        return new WireMockContextStub(new RemoteEndPointConfigRegistry(httpMock, 'http://localhost:8080/base'))
    }
}
