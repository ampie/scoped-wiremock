package com.sbg.bdd.wiremock.scoped.resteasy

import com.sbg.bdd.wiremock.scoped.cdi.internal.EndPointCategoryLiteral
import com.sbg.bdd.wiremock.scoped.cdi.internal.EndPointPropertyLiteral
import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import spock.lang.Specification

class WhenUsingTheDynamicClientRequestFactory extends Specification {
    def 'it should resolve the correct url and'() {

        given:
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.clear()
        BaseDependencyInjectorAdaptor.PROPERTIES.put('prop1','http://somehost:999/base')
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        def epp = new EndPointPropertyLiteral('prop1')
        def epc = new EndPointCategoryLiteral('cat1')
        def crf=new DynamicClientRequestFactoryProducer().getClientRequestFactory(epp,epc)
        when:
        def request =crf.createRelativeRequest('/path')
        then:
        request.uri == 'http://somehost:999/base/path'
        request.getHeadersAsObjects().getFirst(HeaderName.ofTheEndpointCategory())== 'cat1'
    }
}
