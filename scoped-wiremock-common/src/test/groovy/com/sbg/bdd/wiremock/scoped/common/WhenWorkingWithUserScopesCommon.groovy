package com.sbg.bdd.wiremock.scoped.common

import com.github.tomakehurst.wiremock.client.VerificationException
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig

abstract class WhenWorkingWithUserScopesCommon extends ScopedWireMockCommonTest {
    def 'join a user scope'() {
        given: 'I start a global scope with a nested scope'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('android_regression', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + EndpointConfig.ENDPOINT_CONFIG_PATH), 'componentx'))
        def nestedScope = wireMock.startNestedScope(globalScope.correlationPath, 'nested',Collections.emptyMap())
        when: 'I start a scope within which a user will interact with the system'
        def userScope = wireMock.startUserScope(nestedScope.correlationPath, 'user1',Collections.emptyMap())
        then: 'the correlation path must end with the user name preceded with a semicolon'
        userScope.correlationPath == nestedScope.correlationPath +"/:user1"
        wireMock.getCorrelatedScope(userScope.correlationPath) != null
    }
    def 'stop a user scope when the parent scope is stoped'() {
        given: 'I start a global scope with a nested scope'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('android_regression', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + EndpointConfig.ENDPOINT_CONFIG_PATH), 'componentx'))
        def nestedScope = wireMock.startNestedScope(globalScope.correlationPath, 'nested',Collections.emptyMap())
        and: 'I start a scope within which a user will interact with the system'
        def userScope = wireMock.startUserScope(nestedScope.correlationPath, 'user1',Collections.emptyMap())
        when: 'I stop the parent scope'
        wireMock.stopNestedScope(nestedScope.correlationPath,Collections.emptyMap())
        then: 'the user scope must be removed'
        def scope = null
        try {
            scope = wireMock.getCorrelatedScope(userScope.correlationPath)
        }catch(VerificationException ve){
            //on client side
        }
        scope == null
    }
}
