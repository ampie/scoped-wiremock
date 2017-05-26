package com.sbg.bdd.wiremock.scoped.cdi.internal

import com.sbg.bdd.wiremock.scoped.cdi.ExampleClass
import com.sbg.bdd.wiremock.scoped.cdi.DummyBinding
import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry
import com.sbg.bdd.wiremock.scoped.jaxws.OutboundCorrelationPathSOAPHandler
import spock.lang.Specification

import javax.enterprise.inject.spi.AnnotatedField
import javax.enterprise.inject.spi.AnnotatedType
import javax.enterprise.inject.spi.InjectionTarget
import javax.enterprise.inject.spi.ProcessInjectionTarget
import javax.xml.ws.BindingProvider

class WhenDeployingAClassWithWebServiceReferences extends Specification{
    def 'all operations to that web service should be intercepted an populated with the correct endpoint address' (){
        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY=Mock(EndPointRegistry){
            endpointUrlFor(_) >> new URL('http://some.host.com')
        }
        def field = ExampleClass.class.getField('theWebService')
        def example = new ExampleClass(new DummyBinding())
        def annotatedField = Mock(AnnotatedField){
            getJavaMember() >> field
        }
        def annotatedType = Mock(AnnotatedType){
            getFields() >> Collections.singleton(annotatedField)
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
        example.theWebService.doStoff()


        def binding = example.theWebService.binding
        then:
        binding.handlerChain.size() ==1
        binding.handlerChain.get(0) instanceof OutboundCorrelationPathSOAPHandler
        def context = example.theWebService.requestContext
        context[BindingProvider.ENDPOINT_ADDRESS_PROPERTY].toExternalForm() == 'http://some.host.com'
    }
}
