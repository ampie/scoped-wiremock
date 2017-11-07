package com.sbg.bdd.wiremock.scoped.cdi.internal

import com.sbg.bdd.wiremock.scoped.cdi.DummyBinding
import com.sbg.bdd.wiremock.scoped.cdi.ExampleClass
import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.BaseRuntimeCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.EndpointRegistry
import com.sbg.bdd.wiremock.scoped.jaxws.OutboundCorrelationPathSOAPHandler
import org.jboss.security.SecurityContext
import org.jboss.security.SecurityContextAssociation
import spock.lang.Specification

import javax.enterprise.inject.spi.AnnotatedField
import javax.enterprise.inject.spi.AnnotatedType
import javax.enterprise.inject.spi.InjectionTarget
import javax.enterprise.inject.spi.ProcessInjectionTarget
import javax.xml.ws.BindingProvider

class WhenInvokingAClassWithWebServiceReferences extends Specification{
    def 'by default, all operations to that web service should be intercepted an populated with the correct endpoint address' (){
        given:
        DependencyInjectionAdaptorFactory.useAdaptor(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY=Mock(EndpointRegistry){
            endpointUrlFor('my.soap.endpoint.property') >> new URL('http://some.soap.host.com')
        }
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseRuntimeCorrelationState()
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
        new HeaderPropagatingExtension().processInjectionTarget(processInjectionTarget)
        example.theWebService.sayHello('asdf')


        def binding = example.theWebService.binding
        then:
        binding.handlerChain.size() == 1
        binding.handlerChain.get(0) instanceof OutboundCorrelationPathSOAPHandler
        def context = example.theWebService.requestContext
        context[BindingProvider.ENDPOINT_ADDRESS_PROPERTY] == 'http://some.soap.host.com'
    }
    def 'within a scoped wiremockable call, all operations to that web service should be intercepted an populated with an endpoint mocked on the WireMock server' (){
        given:
        DependencyInjectionAdaptorFactory.useAdaptor(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY=Mock(EndpointRegistry){
            endpointUrlFor('my.soap.endpoint.property') >> new URL('http://some.soap.host.com')
        }
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseRuntimeCorrelationState()
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.set("wiremock-host/8080/some/scope/path",1,false)
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
        new HeaderPropagatingExtension().processInjectionTarget(processInjectionTarget)
        example.theWebService.sayHello('asdf')


        def binding = example.theWebService.binding
        then:
        binding.handlerChain.size() == 1
        binding.handlerChain.get(0) instanceof OutboundCorrelationPathSOAPHandler
        def context = example.theWebService.requestContext
        context[BindingProvider.ENDPOINT_ADDRESS_PROPERTY] == 'http://wiremock-host:8080'
    }
    def 'a WebService reference should be created automatically using the Service implementation in the WebService annotation if the reference is null' (){
        given:
        DependencyInjectionAdaptorFactory.useAdaptor(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY=Mock(EndpointRegistry){
            endpointUrlFor('my.soap.endpoint.property') >> new URL('http://some.soap.host.com')
        }
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseRuntimeCorrelationState()
        def field = ExampleClass.class.getField('theWebService')
        def example = new ExampleClass()
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
        new HeaderPropagatingExtension().processInjectionTarget(processInjectionTarget)
        example.theWebService.sayHello('asdf')


        def binding = example.theWebService.binding
        then:
        binding.handlerChain.size() == 1
        binding.handlerChain.get(0) instanceof OutboundCorrelationPathSOAPHandler
        def context = example.theWebService.requestContext
        context[BindingProvider.ENDPOINT_ADDRESS_PROPERTY] == 'http://some.soap.host.com'
    }
    def 'the developer should be able to create a WebService reference programmatically' (){
        given:
        DependencyInjectionAdaptorFactory.useAdaptor(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY=Mock(EndpointRegistry){
            endpointUrlFor('my.soap.endpoint.property') >> new URL('http://some.soap.host.com')
        }
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseRuntimeCorrelationState()
        def field = ExampleClass.class.getField('theWebService')
        def example = new ExampleClass()
        example.theWebService=EndPointHelper.wrapEndpoint(new DummyBinding(),'my.soap.endpoint.property','some-category')
        when:

        example.theWebService.sayHello('asdf')


        def binding = example.theWebService.binding
        then:
        binding.handlerChain.size() == 1
        binding.handlerChain.get(0) instanceof OutboundCorrelationPathSOAPHandler
        def context = example.theWebService.requestContext
        context[BindingProvider.ENDPOINT_ADDRESS_PROPERTY] == 'http://some.soap.host.com'
    }

    def 'the OutboundCorrelationPathSOAPHandler should be added once only to the handler chain' (){
        given:
        DependencyInjectionAdaptorFactory.useAdaptor(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY=Mock(EndpointRegistry){
            endpointUrlFor('my.soap.endpoint.property') >> new URL('http://some.soap.host.com')
        }
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseRuntimeCorrelationState()
        def field = ExampleClass.class.getField('theWebService')
        def dummyBinding = new DummyBinding()
        dummyBinding.binding.handlerChain.add(new OutboundCorrelationPathSOAPHandler())
        def example = new ExampleClass(dummyBinding)
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
        new HeaderPropagatingExtension().processInjectionTarget(processInjectionTarget)
        example.theWebService.sayHello('asdf')


        def binding = example.theWebService.binding
        then:
        binding.handlerChain.size() == 1
        binding.handlerChain.get(0) instanceof OutboundCorrelationPathSOAPHandler
        def context = example.theWebService.requestContext
        context[BindingProvider.ENDPOINT_ADDRESS_PROPERTY] == 'http://some.soap.host.com'
    }
}
