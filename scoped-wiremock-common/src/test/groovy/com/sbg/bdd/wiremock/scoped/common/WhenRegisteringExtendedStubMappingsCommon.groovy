package com.sbg.bdd.wiremock.scoped.common

import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedRequestPattern
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedResponseDefinition
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedStubMapping
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState
import com.sbg.bdd.wiremock.scoped.admin.model.ScopeLocalPriority
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import com.sbg.bdd.wiremock.scoped.integration.HttpCommandExecutor

import static com.github.tomakehurst.wiremock.client.WireMock.*

abstract class WhenRegisteringExtendedStubMappingsCommon extends ScopedWireMockCommonTest {
    def 'ExtendedStubMappings are always registered at a specific scope which results in a specific priority '() {
        given: 'I have a new global scope with a nested scope'
        def rootScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('android_regression', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'componentx'))
        def nestedScope = wireMock.joinCorrelatedScope(rootScope.correlationPath, 'nested_scope', Collections.emptyMap())

        when: 'I register one mapping in the nested scope at priority BODY_KNOWN'
        def originalMapping = get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(nestedScope.correlationPath + '.*')).willReturn(aResponse()).build()
        def extendedMapping = new ExtendedStubMapping(new ExtendedRequestPattern(nestedScope.correlationPath, originalMapping.request), new ExtendedResponseDefinition(originalMapping.response))
        extendedMapping.localPriority = ScopeLocalPriority.BODY_KNOWN
        wireMock.register(extendedMapping)

        then: 'the nested scope will have 1 mappings'
        def mappingsInScope = wireMock.getMappingsInScope(nestedScope.correlationPath)
        mappingsInScope.size() == 1
        and: 'its priority will be 93'
        mappingsInScope[0].priority == 93

    }

    def 'ExtendedStubMappings that target the property name of a specific EndPointConfig will result in a StubMapping with the resolved path'() {
        given: 'I have a new global scope with a nested scope'
        def rootScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('android_regression', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), null))
        def nestedScope = wireMock.joinCorrelatedScope(rootScope.correlationPath, 'nested_scope', Collections.emptyMap())
        and: "'an EndPointConfig for the property 'my.property.name'"
        HttpCommandExecutor.INSTANCE = Mock(HttpCommandExecutor) {
            execute(_) >> { args ->
                if (args[0].url.path == '/sut' + EndpointConfig.ENDPOINT_CONFIG_PATH+ 'all') {
                    return '[{"propertyName":"my.property.name","url":"http://host1.com/some/path","endpointType":"REST","categories":["category1"],"scopes":[]}]'
                } else {
                    throw new IllegalArgumentException(args[0].url.path);
                }
            }
        }

        when: 'I register one mapping in the nested scope with the property name my.property.name'
        def originalMapping = get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(nestedScope.correlationPath + '.*')).willReturn(aResponse()).build()
        def extendedMapping = new ExtendedStubMapping(new ExtendedRequestPattern(nestedScope.correlationPath, originalMapping.request), new ExtendedResponseDefinition(originalMapping.response))
        extendedMapping.request.urlInfo = 'my.property.name'
        extendedMapping.localPriority = ScopeLocalPriority.BODY_KNOWN
        wireMock.register(extendedMapping)

        then: "the nested scope will have a single mapping reflecting the path segment '/some/path' from the original url of the endpoint"
        def mappingsInScope = wireMock.getMappingsInScope(nestedScope.correlationPath)
        mappingsInScope.size() == 1
        mappingsInScope[0].request.urlPath == '/some/path'
    }


    def 'ExtendedStubMappings that intercept requests targeting a specific EndPointConfig will result in a StubMapping with the resolved path of the EndPointConfig'() {
        given: 'I have a new global scope with a nested scope'
        def rootScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('android_regression', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), null))
        def nestedScope = wireMock.joinCorrelatedScope(rootScope.correlationPath, 'nested_scope', Collections.emptyMap())
        and: "'an EndPointConfig for the property 'my.property.name'"
        HttpCommandExecutor.INSTANCE = Mock(HttpCommandExecutor) {
            execute(_) >> { args ->
                if (args[0].url.path == '/sut' + EndpointConfig.ENDPOINT_CONFIG_PATH+ 'all') {
                    return '[{"propertyName":"my.property.name","url":"http://host1.com/some/path","endpointType":"REST","categories":["category1"],"scopes":[]}]'
                } else {
                    throw new IllegalArgumentException(args[0].url.path);
                }
            }
        }

        when: 'I register one mapping in the nested scope with the property name my.property.name'
        def originalMapping = get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(nestedScope.correlationPath + '.*')).willReturn(aResponse()).build()
        def response = new ExtendedResponseDefinition(originalMapping.response)
        response.interceptFromSource=true
        def extendedMapping = new ExtendedStubMapping(new ExtendedRequestPattern(nestedScope.correlationPath, originalMapping.request), response)
        extendedMapping.request.urlInfo = 'my.property.name'
        extendedMapping.localPriority = ScopeLocalPriority.BODY_KNOWN
        wireMock.register(extendedMapping)

        then: "the nested scope will have a single mapping reflecting the path segment '/some/path' from the original url of the endpoint"
        def mappingsInScope = wireMock.getMappingsInScope(nestedScope.correlationPath)
        mappingsInScope.size() == 1
        mappingsInScope[0].request.urlPath == '/some/path'
        and: "a proxy mapping that proxies to the original service at http://host1.com"
        mappingsInScope[0].response.proxyBaseUrl == 'http://host1.com'
    }

    def 'ExtendedStubMappings that target all known external services will result in a StubMapping with the resolved path for each known EndPointConfig'() {
        given: 'I have a new global scope with a nested scope'
        def rootScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('android_regression', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), null))
        def nestedScope = wireMock.joinCorrelatedScope(rootScope.correlationPath, 'nested_scope', Collections.emptyMap())
        and: "four configured EndPointConfigs"
        HttpCommandExecutor.INSTANCE = Mock(HttpCommandExecutor) {
            execute(_) >> { args ->
                if (args[0].url.path == '/sut' + EndpointConfig.ENDPOINT_CONFIG_PATH+ 'all') {
                    return '[' +
                            '{"propertyName":"endpoint1","url":"http://host1.com/some/path1","endpointType":"REST","categories":["category1"],"scopes":[]},' +
                            '{"propertyName":"endpoint2","url":"http://host2.com/some/path2","endpointType":"SOAP","categories":["category1"],"scopes":[]},' +
                            '{"propertyName":"endpoint3","url":"http://host3.com/some/path3","endpointType":"REST","categories":["category2"],"scopes":[]},' +
                            '{"propertyName":"endpoint4","url":"http://host4.com/some/path4","endpointType":"SOAP","categories":["category2"],"scopes":[]}' +
                            ']'
                } else {
                    throw new IllegalArgumentException(args[0].url.path);
                }
            }
        }

        when: 'I register one mapping in the nested scope that targes all known external services'
        def originalMapping = get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(nestedScope.correlationPath + '.*')).willReturn(aResponse()).build()
        def extendedMapping = new ExtendedStubMapping(new ExtendedRequestPattern(nestedScope.correlationPath, originalMapping.request), new ExtendedResponseDefinition(originalMapping.response))
        extendedMapping.request.toAllKnownExternalServices = true
        extendedMapping.localPriority = ScopeLocalPriority.BODY_KNOWN
        wireMock.register(extendedMapping)

        then: "the nested scope will have four mappings reflecting the path segment of each of the original endpoint urls"
        def mappingsInScope = wireMock.getMappingsInScope(nestedScope.correlationPath)
        mappingsInScope.size() == 4
        and: "REST endpoints will have wildcard match urlPatterns"
        mappingsInScope[3].request.urlPathPattern == '/some/path1.*'
        mappingsInScope[1].request.urlPathPattern == '/some/path3.*'
        and: "SOAP endpoints will have exact match urlPatterns"
        mappingsInScope[2].request.urlPath == '/some/path2'
        mappingsInScope[0].request.urlPath == '/some/path4'
    }
    def 'ExtendedStubMappings that target known external services of a specific category will result in StubMappings with the resolved path for each known EndPointConfig that belongs to that category'() {
        given: 'I have a new global scope with a nested scope'
        def rootScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('android_regression', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), null))
        def nestedScope = wireMock.joinCorrelatedScope(rootScope.correlationPath, 'nested_scope', Collections.emptyMap())
        and: "four configured EndPointConfigs"
        HttpCommandExecutor.INSTANCE = Mock(HttpCommandExecutor) {
            execute(_) >> { args ->
                if (args[0].url.path == '/sut' + EndpointConfig.ENDPOINT_CONFIG_PATH+ 'all') {
                    return '[' +
                            '{"propertyName":"endpoint1","url":"http://host1.com/some/path1","endpointType":"REST","categories":["category1"],"scopes":[]},' +
                            '{"propertyName":"endpoint2","url":"http://host2.com/some/path2","endpointType":"SOAP","categories":["category1"],"scopes":[]},' +
                            '{"propertyName":"endpoint3","url":"http://host3.com/some/path3","endpointType":"REST","categories":["category2"],"scopes":[]},' +
                            '{"propertyName":"endpoint4","url":"http://host4.com/some/path4","endpointType":"SOAP","categories":["category2"],"scopes":[]}' +
                            ']'
                } else {
                    throw new IllegalArgumentException(args[0].url.path);
                }
            }
        }

        when: 'I register one mapping in the nested scope that targes all known external services of category2'
        def originalMapping = get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(nestedScope.correlationPath + '.*')).willReturn(aResponse()).build()
        def extendedMapping = new ExtendedStubMapping(new ExtendedRequestPattern(nestedScope.correlationPath, originalMapping.request), new ExtendedResponseDefinition(originalMapping.response))
        extendedMapping.request.toAllKnownExternalServices = true
        extendedMapping.request.endpointCategory = 'category2'
        extendedMapping.localPriority = ScopeLocalPriority.BODY_KNOWN
        wireMock.register(extendedMapping)

        then: "the nested scope will have four mappings reflecting the path segment of each of the original endpoint urls from that category"
        def mappingsInScope = wireMock.getMappingsInScope(nestedScope.correlationPath)
        mappingsInScope.size() == 2
        and: "REST endpoints will have wildcard match urlPatterns"
        mappingsInScope[1].request.urlPathPattern == '/some/path3.*'
        and: "SOAP endpoints will have exact match urlPatterns"
        mappingsInScope[0].request.urlPath == '/some/path4'
    }


}
