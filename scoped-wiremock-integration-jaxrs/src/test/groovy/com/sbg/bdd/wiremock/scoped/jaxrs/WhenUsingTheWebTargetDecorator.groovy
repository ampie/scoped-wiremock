package com.sbg.bdd.wiremock.scoped.jaxrs

import com.sbg.bdd.wiremock.scoped.cdi.internal.EndPointCategoryLiteral
import com.sbg.bdd.wiremock.scoped.cdi.internal.EndPointPropertyLiteral
import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.BaseWireMockCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import spock.lang.Specification

import javax.ws.rs.client.ClientBuilder

class WhenUsingTheWebTargetDecorator extends Specification {
    def 'it should point to the original the endpoint when there is no correlation context'() {
        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.PROPERTIES.put("prop1", "http://somehost:9090/some/base/path")
        def state = BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE = new BaseWireMockCorrelationState()
//        state.set('localhost/8080/somepath', true)
        state.clear()
        def decorator = new DynamicWebTarget(ClientBuilder.newClient(),new EndPointPropertyLiteral("prop1"),new EndPointCategoryLiteral("cat1"))
        when:
        def invocation=decorator.path("/last/segment").request().buildGet()

        then:
        invocation.uri.toString() == 'http://somehost:9090/some/base/path/last/segment'
        invocation.headers.getHeader(HeaderName.ofTheEndpointCategory()) == null

    }
    def 'it should point to wiremock and add headers when there a correlation context'() {
        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.PROPERTIES.put("prop1", "http://somehost:9090/some/base/path")
        def state = BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE = new BaseWireMockCorrelationState()
        state.set('localhost/8080/somepath', true)
        def decorator = new DynamicWebTarget(ClientBuilder.newClient(),new EndPointPropertyLiteral("prop1"),new EndPointCategoryLiteral("cat1"))
        when:
        def invocation=decorator.path("/last/segment").request().buildGet()

        then:
        invocation.uri.toString() == 'http://localhost:8080/some/base/path/last/segment'
        invocation.headers.getHeader(HeaderName.ofTheEndpointCategory()) == 'cat1'

    }
}
