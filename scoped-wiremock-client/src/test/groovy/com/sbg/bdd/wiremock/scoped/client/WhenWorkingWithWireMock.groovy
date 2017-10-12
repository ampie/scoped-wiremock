package com.sbg.bdd.wiremock.scoped.client

import com.sbg.bdd.wiremock.scoped.admin.endpointconfig.RemoteEndPointConfigRegistry
import com.sbg.bdd.wiremock.scoped.integration.HttpCommand
import com.sbg.bdd.wiremock.scoped.integration.HttpCommandExecutor
import groovy.json.JsonOutput

import spock.lang.Specification

abstract class WhenWorkingWithWireMock extends Specification {
    public static final int MAX_LEVELS = 10;
    public static final int PRIORITIES_PER_LEVEL = 10;
    static final int EVERYBODY_PRIORITY_DECREMENT = PRIORITIES_PER_LEVEL / 2;

    def initializeWireMockContext() {
        HttpCommandExecutor.INSTANCE=Mock(HttpCommandExecutor) {
            execute(_) >> { args ->
                HttpCommand request = args[0]
                def body = null;
                if (request.url.toExternalForm().endsWith('/Property/all')) {
                    body = JsonOutput.toJson([configs: [
                            [propertyName: 'external.service.a', url: 'http://somehost.com/service/one/endpoint', endpointType: 'REST', categories:['category1']],
                            [propertyName: 'external.service.b', url: 'http://somehost.com/service/two/endpoint', endpointType: 'SOAP', categories: ['category1']]

                    ]])
                } else {
                    body = JsonOutput.toJson([propertyName: 'x', url: 'http://somehost.com/resolved/endpoint', endpointType: 'SOAP', category: 'category1'])
                }
                return body
            }
        }
        return new WireMockContextStub(new RemoteEndPointConfigRegistry('http://some.host/rest/', 'all'))
    }
}
