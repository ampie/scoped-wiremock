package com.sbg.bdd.wiremock.scoped.resteasy

import com.sbg.bdd.wiremock.scoped.cdi.internal.MockableEndPointLiteral
import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import spock.lang.Specification

class WhenUsingTheDynamicClientRequestFactory extends Specification {
    def 'it should resolve the correct url and'() {

        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.clear()
        BaseDependencyInjectorAdaptor.PROPERTIES.put('prop1','http://somehost:999/base')
        def epp = new MockableEndPointLiteral('prop1','cat1 cat2'.split(), )
        def crf=new DynamicClientRequestFactoryProducer().getClientRequestFactory(epp)
        when:
        def request =crf.createRelativeRequest('/path')
        then:
        request.uri == 'http://somehost:999/base/path'
        request.getHeadersAsObjects().getFirst(HeaderName.ofTheEndpointCategory())== 'cat1'
        request.getHeadersAsObjects().get(HeaderName.ofTheEndpointCategory()).get(1)== 'cat2'
    }
}
