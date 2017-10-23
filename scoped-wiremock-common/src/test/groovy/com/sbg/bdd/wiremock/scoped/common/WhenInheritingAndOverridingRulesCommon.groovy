package com.sbg.bdd.wiremock.scoped.common

import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.HeaderName

import static com.github.tomakehurst.wiremock.client.WireMock.*

abstract class WhenInheritingAndOverridingRulesCommon extends ScopedWireMockCommonTest {
    def 'nested scopes inherit mappings from their containing scopes'() {
        given: 'I have a new global scope with a nested scope'
        def urlOfSut = new URL(wireMock.baseUrl() + '/sut1')
        def rootScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('android_regression', new URL(wireMock.baseUrl()), urlOfSut, 'componentx'))
        def nestedScope = wireMock.joinCorrelatedScope(rootScope.correlationPath , 'nested_scope', Collections.emptyMap())

        when: 'I register one mapping in each scope'
        wireMock.register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(rootScope.correlationPath + '.*')).willReturn(aResponse()).atPriority(1));
        wireMock.register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(nestedScope.getCorrelationPath() + ".*")).willReturn(aResponse()).atPriority(1));

        then: 'the root scope will have 2 mappings in scope'
        wireMock.getMappingsInScope(rootScope.correlationPath).size() == 1
        and: 'the nested scope will have 1 mapping in scope'
        wireMock.getMappingsInScope(nestedScope.correlationPath).size() == 2

    }

    def 'mappings are removed from nested scopes when their containing scopes are removed'() {
        given:'I register one mapping each for a global and nested scope'
        def urlOfSut = new URL(wireMock.baseUrl() + '/sut1')
        def rootScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('android_regression', new URL(wireMock.baseUrl()), urlOfSut, 'componentx'))
        def nestedScope = wireMock.joinCorrelatedScope(rootScope.correlationPath , 'nested_scope', Collections.emptyMap())
        wireMock.register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(rootScope.correlationPath + '.*')).willReturn(aResponse()).atPriority(1));
        wireMock.register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(nestedScope.correlationPath + '.*')).willReturn(aResponse()).atPriority(1));

        when:'I stop the containing scope'
        wireMock.stopGlobalScope(rootScope)

        then: 'the both scopes will have 0 mappings in scope'
        wireMock.getMappingsInScope(rootScope.correlationPath).size() == 0
        wireMock.getMappingsInScope(nestedScope.correlationPath).size() == 0
    }
}
