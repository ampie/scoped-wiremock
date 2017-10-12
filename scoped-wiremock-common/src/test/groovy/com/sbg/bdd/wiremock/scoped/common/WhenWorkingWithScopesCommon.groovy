package com.sbg.bdd.wiremock.scoped.common

import com.github.tomakehurst.wiremock.client.VerificationException
import com.sbg.bdd.wiremock.scoped.admin.endpointconfig.RemoteEndPointConfigRegistry
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState

import static org.spockframework.util.Assert.fail

abstract class WhenWorkingWithScopesCommon extends ScopedWireMockCommonTest {
    def 'start a new global scope'() {
        given: 'a service under test'
        def urlOfSut = new URL(wireMock.baseUrl() + RemoteEndPointConfigRegistry.ENDPOINT_CONFIG_PATH)
        when: 'I start a new global scope'
        def scope = wireMock.startNewGlobalScope('android_regression', new URL(wireMock.baseUrl()), urlOfSut, 'componentx')
        then: 'the correlation path must reflect the WireMock host name, port, the testRunName and a 0 number'
        scope.correlationPath == wireMock.host() + '/' + wireMock.port() + '/android_regression/0'
    }

    def 'start a second global scope'() {
        given: 'a service under test'
        def urlOfSut = new URL(wireMock.baseUrl() + RemoteEndPointConfigRegistry.ENDPOINT_CONFIG_PATH)
        when: 'I start a new global scope'
        wireMock.startNewGlobalScope('android_regression', new URL(wireMock.baseUrl()), urlOfSut, 'componentx')
        def scope = wireMock.startNewGlobalScope('android_regression', new URL(wireMock.baseUrl()), urlOfSut, 'componentx')
        then: 'the correlation path must reflect the WireMock host name, port, the testRunName and a sequence number of 1'
        scope.correlationPath == wireMock.host() + '/' + wireMock.port() + '/android_regression/1'
    }

    def 'start a second global scope after removing the first scope'() {
        given: 'a service under test'
        def urlOfSut = new URL(wireMock.baseUrl() + RemoteEndPointConfigRegistry.ENDPOINT_CONFIG_PATH)
        when: 'I start a new global scope'
        wireMock.startNewGlobalScope('android_regression', new URL(wireMock.baseUrl()), urlOfSut, 'componentx')
        wireMock.stopGlobalScope('android_regression', new URL(wireMock.baseUrl()), 0)
        def scope = wireMock.startNewGlobalScope('android_regression', new URL(wireMock.baseUrl()), urlOfSut, 'componentx')
        then: 'the correlation path must reflect the WireMock host name, port, the testRunName and a sequence number of 0'
        scope.correlationPath == wireMock.host() + '/' + wireMock.port() + '/android_regression/0'
    }

    def 'start or join a nested scope'() throws IOException {
        given: 'I have started a global scope'
        def urlOfSut = new URL(wireMock.baseUrl() + RemoteEndPointConfigRegistry.ENDPOINT_CONFIG_PATH)
        def globalCorrelationPath = wireMock.startNewGlobalScope('android_regression', new URL(wireMock.baseUrl()), urlOfSut, 'componentx').correlationPath
        when: 'I start a nested scope'
        def resultScope = getWireMock().joinCorrelatedScope(globalCorrelationPath + '/my_nested_scope', Collections.singletonMap('someKey', 'someValue'))
        then: 'the resulting correlation path should both the global scope path and the name of the nested scope and reflect the payload'
        resultScope.correlationPath == globalCorrelationPath + '/my_nested_scope'
        resultScope.payload['someKey'] == 'someValue'//Really? not sure
    }


    def 'keep track of the service invocation counts per scope'() {
        given: 'I have started a nested scope with no service invocation counts'
        def urlOfSut = new URL(wireMock.baseUrl() + RemoteEndPointConfigRegistry.ENDPOINT_CONFIG_PATH)
        def globalCorrelationPath = wireMock.startNewGlobalScope('android_regression', new URL(wireMock.baseUrl()), urlOfSut, 'componentx').correlationPath
        wireMock.joinCorrelatedScope(globalCorrelationPath + '/my_nested_scope', Collections.singletonMap('someKey', 'someValue'))

        when: 'I update the service invocation counts for service1 to 2 and service2 to 1'
        def nestedScope = new CorrelationState(globalCorrelationPath + '/my_nested_scope')
        nestedScope.serviceInvocationCounts['service1'] = 2
        nestedScope.serviceInvocationCounts['service2'] = 1
        wireMock.syncCorrelatedScope(nestedScope)

        then: 'these new values should reflect'
        def responseToGet = wireMock.getCorrelatedScope(globalCorrelationPath + '/my_nested_scope');
        responseToGet.serviceInvocationCounts['service1'] == 2
        responseToGet.serviceInvocationCounts['service2'] == 1
    }

    def 'complete a nested scope'() {
        given: 'I have started a nested scope'
        def urlOfSut = new URL(wireMock.baseUrl() + RemoteEndPointConfigRegistry.ENDPOINT_CONFIG_PATH)
        def globalCorrelationPath = wireMock.startNewGlobalScope('android_regression', new URL(wireMock.baseUrl()), urlOfSut, 'componentx').correlationPath
        wireMock.joinCorrelatedScope(globalCorrelationPath + '/my_nested_scope', Collections.singletonMap('someKey', 'someValue'))

        when:
        def removedScopes = getWireMock().stopCorrelatedScope(globalCorrelationPath + '/my_nested_scope')

        then:
        removedScopes.size() == 1
        removedScopes[0] == globalCorrelationPath + '/my_nested_scope'
        try {
            if (wireMock.getCorrelatedScope(globalCorrelationPath + '/my_nested_scope') != null) {
                fail('Scope should not exist any more!');
            }
        } catch (VerificationException e) {//Happens in client tests due to 404
        }
    }

    def 'complete a nested scope when completing the parent scope'() {
        given: 'I have started a nested scope'
        def urlOfSut = new URL(wireMock.baseUrl() + RemoteEndPointConfigRegistry.ENDPOINT_CONFIG_PATH)
        def globalCorrelationPath = wireMock.startNewGlobalScope('android_regression', new URL(wireMock.baseUrl()), urlOfSut, 'componentx').correlationPath
        wireMock.joinCorrelatedScope(globalCorrelationPath + '/my_nested_scope', Collections.singletonMap('someKey', 'someValue'))
        wireMock.joinCorrelatedScope(globalCorrelationPath + '/my_nested_scope/nested_nested_scope', Collections.singletonMap('someKey', 'someValue'))

        when:
        def removedScopes = getWireMock().stopCorrelatedScope(globalCorrelationPath + '/my_nested_scope')

        then:
        removedScopes.size() == 2
        removedScopes[0] == globalCorrelationPath + '/my_nested_scope/nested_nested_scope'
        removedScopes[1] == globalCorrelationPath + '/my_nested_scope'
        try {
            if (wireMock.getCorrelatedScope(globalCorrelationPath + '/my_nested_scope/nested_nested_scope') != null) {
                fail('Scope should not exist any more!');
            }
        } catch (VerificationException e) {//Happens in client tests due to 404
        }
    }
}
