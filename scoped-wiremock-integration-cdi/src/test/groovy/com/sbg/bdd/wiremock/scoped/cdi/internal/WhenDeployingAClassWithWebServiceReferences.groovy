package com.sbg.bdd.wiremock.scoped.cdi.internal

import com.sbg.bdd.wiremock.scoped.cdi.ExampleClass
import com.sbg.bdd.wiremock.scoped.cdi.DummyBinding
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig
import com.sbg.bdd.wiremock.scoped.filter.ServerSideEndPointConfigRegistry
import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry
import spock.lang.Specification

import javax.enterprise.inject.spi.AnnotatedField
import javax.enterprise.inject.spi.AnnotatedType
import javax.enterprise.inject.spi.InjectionTarget
import javax.enterprise.inject.spi.ProcessInjectionTarget

class WhenDeployingAClassWithWebServiceReferences extends Specification{

    def 'the endpoint config registry should be updated with all appropriate REST and SOAP endpoints' (){
        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY=Mock(EndPointRegistry){
            endpointUrlFor('my.soap.endpoint.property') >> new URL('http://some.soap.host.com')
            endpointUrlFor('my.rest.endpoint.property') >> new URL('http://some.rest.host.com')
        }
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new RequestScopedWireMockCorrelationState()
        def example = new ExampleClass(new DummyBinding())
        example.theRestService=new Object()
        def webReferenceField = Mock(AnnotatedField){
            getJavaMember() >> ExampleClass.class.getField('theWebService')
        }
        def restReferenceField = Mock(AnnotatedField){
            getJavaMember() >> ExampleClass.class.getField('theRestService')
        }
        def annotatedType = Mock(AnnotatedType){
            getFields() >> new HashSet<>(Arrays.asList(webReferenceField,restReferenceField))
        }
        def processInjectionTarget = Mock(ProcessInjectionTarget){
            getAnnotatedType() >> annotatedType
            getInjectionTarget() >> Mock(InjectionTarget)
            setInjectionTarget(_) >> {args->
                args[0].inject(example,null)
            }
        }

        when:
        new DynamicWebServiceEndPointExtension().processInjectionTarget(processInjectionTarget)

        then:
        ServerSideEndPointConfigRegistry.instance.allEndpointConfigs.size() == 2
        def soapConfig = ServerSideEndPointConfigRegistry.instance.getEndpointConfig('my.soap.endpoint.property')
        soapConfig.endpointType == EndpointConfig.EndpointType.SOAP
        soapConfig.url == new URL('http://some.soap.host.com')
        soapConfig.categories[0] == "cat1"
        soapConfig.categories[1] == "cat2"
        soapConfig.scope[0] == "scope1"
        soapConfig.scope[1] == "scope2"
    }

}
