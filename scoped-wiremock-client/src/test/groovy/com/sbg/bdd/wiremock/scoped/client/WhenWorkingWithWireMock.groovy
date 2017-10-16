package com.sbg.bdd.wiremock.scoped.client

import com.sbg.bdd.wiremock.scoped.admin.endpointconfig.RemoteEndpointConfigRegistry
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig
import com.sbg.bdd.wiremock.scoped.integration.HttpCommand
import com.sbg.bdd.wiremock.scoped.integration.HttpCommandExecutor
import com.sbg.bdd.wiremock.scoped.server.CorrelatedScope
import com.sbg.bdd.wiremock.scoped.server.GlobalScope
import groovy.json.JsonOutput

import spock.lang.Specification

abstract class WhenWorkingWithWireMock extends Specification {
    public static final int MAX_LEVELS = 10;
    public static final int PRIORITIES_PER_LEVEL = 10;
    static final int EVERYBODY_PRIORITY_DECREMENT = PRIORITIES_PER_LEVEL / 2;

    def initializeWireMockContext() {
        HttpCommandExecutor.INSTANCE = Mock(HttpCommandExecutor) {
            execute(_) >> { args ->
                return '[' +
                        '{"propertyName":"external.service.a","url":"http://somehost.com/service/one/endpoint","endpointType":"REST","categories":["category1"],"scopes":[]},' +
                        '{"propertyName":"external.service.b","url":"http://somehost.com/service/two/endpoint","endpointType":"SOAP","categories":["category1"],"scopes":[]}' +
                        ']'
            }
        }
        def registry = new RemoteEndpointConfigRegistry('http://some.host/rest/', EndpointConfig.LOCAL_INTEGRATION_SCOPE)
        return new WireMockContextStub(Mock(CorrelatedScope) {
            getLevel() >> 1
            getGlobalScope() >> Mock(GlobalScope) {
                getEndPointConfigRegistry() >> registry
            }
        })
    }
}
