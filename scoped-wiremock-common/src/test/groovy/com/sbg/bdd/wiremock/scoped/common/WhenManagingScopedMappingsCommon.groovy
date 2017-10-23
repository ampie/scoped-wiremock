package com.sbg.bdd.wiremock.scoped.common

import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.HeaderName

import static com.github.tomakehurst.wiremock.client.WireMock.*

abstract class WhenManagingScopedMappingsCommon extends ScopedWireMockCommonTest {
    def 'a mapping should be add to one single scope only'() {
        given: 'I have two scopes'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx'))

        def scope1 = wireMock.joinCorrelatedScope(globalScope.correlationPath , 'scope1', Collections.emptyMap());
        def scope2 = wireMock.joinCorrelatedScope(globalScope.correlationPath , 'scope2', Collections.emptyMap());
        when: 'I add a mapping to the first scope'
        wireMock.register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching( scope1.correlationPath +'.*')).willReturn(aResponse()).atPriority(1));
        then: 'the first scope should contain that mapping, not the second scope'
        wireMock.getMappingsInScope(scope1.correlationPath).size() == 1
        wireMock.getMappingsInScope(scope2.correlationPath).size() == 0
    }


    def 'a mapping should be removed from its containing scope when the scope stops'(){
        given: 'I have two scopes and I have added a mapping to each'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx'))

        def scope1 = wireMock.joinCorrelatedScope(globalScope.correlationPath , 'scope1', Collections.emptyMap());
        def scope2 = wireMock.joinCorrelatedScope(globalScope.correlationPath , 'scope2', Collections.emptyMap());
        wireMock.register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(scope1.correlationPath + '.*')).willReturn(aResponse()).atPriority(1));
        wireMock.register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(scope2.correlationPath + '.*')).willReturn(aResponse()).atPriority(1));
        when: 'I stop the second scope'
        wireMock.stopNestedScope(scope2.correlationPath,Collections.emptyMap())
        then: 'the first scope should still have its mapping'
        wireMock.getMappingsInScope(scope1.correlationPath).size() == 1
        wireMock.getMappingsInScope(scope2.correlationPath).size() == 0
    }

}
