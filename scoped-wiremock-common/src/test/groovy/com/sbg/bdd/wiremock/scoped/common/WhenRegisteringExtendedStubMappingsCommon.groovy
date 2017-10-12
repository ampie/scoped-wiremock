package com.sbg.bdd.wiremock.scoped.common

import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedRequestPattern
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedResponseDefinition
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedStubMapping
import com.sbg.bdd.wiremock.scoped.admin.model.ScopeLocalPriority
import com.sbg.bdd.wiremock.scoped.integration.HeaderName

import static com.github.tomakehurst.wiremock.client.WireMock.*

abstract class WhenRegisteringExtendedStubMappingsCommon extends ScopedWireMockCommonTest {
    def 'registering '() {
        given: 'I have a new global scope with a nested scope'
        def rootScope = wireMock.startNewGlobalScope('android_regression', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() +'/sut'), 'componentx')
        def nestedScope = wireMock.joinCorrelatedScope(rootScope.correlationPath + '/nested_scope', Collections.emptyMap())

        when: 'I register one mapping in the nested scope at priority BODY_KNOWN'
        def originalMapping = get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(nestedScope.correlationPath + '.*')).willReturn(aResponse()).build()
        def extendedMapping = new ExtendedStubMapping(nestedScope.correlationPath,new ExtendedRequestPattern(originalMapping.request),new ExtendedResponseDefinition(originalMapping.response))
        extendedMapping.localPriority = ScopeLocalPriority.BODY_KNOWN
        wireMock.register(extendedMapping)

        then: 'the nested scope will have 1 mappings'
        def mappingsInScope = wireMock.getMappingsInScope(nestedScope.correlationPath)
        mappingsInScope.size() == 1
        and: 'its priority will be 93'
        mappingsInScope[0].priority == 93

    }

}
