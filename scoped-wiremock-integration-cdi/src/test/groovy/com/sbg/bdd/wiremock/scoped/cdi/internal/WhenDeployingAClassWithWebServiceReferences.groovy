package com.sbg.bdd.wiremock.scoped.cdi.internal

import com.sbg.bdd.wiremock.scoped.cdi.AsyncBean
import com.sbg.bdd.wiremock.scoped.cdi.ExampleClass
import com.sbg.bdd.wiremock.scoped.cdi.DummyBinding
import com.sbg.bdd.wiremock.scoped.integration.BaseWireMockCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig
import com.sbg.bdd.wiremock.scoped.filter.ServerSideEndPointConfigRegistry
import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.EndpointRegistry
import org.jboss.security.SecurityContextAssociation
import org.jboss.security.plugins.JBossSecurityContext
import spock.lang.Specification

import javax.enterprise.inject.spi.AnnotatedField
import javax.enterprise.inject.spi.AnnotatedType
import javax.enterprise.inject.spi.InjectionTarget
import javax.enterprise.inject.spi.ProcessInjectionTarget

class WhenDeployingAClassWithWebServiceReferences extends Specification{

    def 'the endpoint config registry should be updated with all appropriate REST and SOAP endpoints' (){
        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY=Mock(EndpointRegistry){
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

        when:'deploying the example bean'
        new HeaderPropagatingExtension().processInjectionTarget(processInjectionTarget)

        then:
        println 'getting client configs'
        ServerSideEndPointConfigRegistry.instance.allEndpointConfigs.size() == 2
        println 'getting soap endpoint config configs'
        def soapConfig = ServerSideEndPointConfigRegistry.instance.getEndpointConfig('my.soap.endpoint.property')
        soapConfig.endpointType == EndpointConfig.EndpointType.SOAP
        soapConfig.url == new URL('http://some.soap.host.com')
        soapConfig.categories[0] == "cat1"
        soapConfig.categories[1] == "cat2"
        soapConfig.scopes[0] == "scope1"
        soapConfig.scopes[1] == "scope2"
    }
    def 'references to async beans with the PropagatesHeaders annotation should be wrapped in a proxy ' (){
        //TODO is there a more direct way to test this? This sounds like a WhenInvoking.... test
        given:
        SecurityContextAssociation.setSecurityContext(new JBossSecurityContext('asdf'))
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new RequestScopedWireMockCorrelationState()
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.set("localhost/123/runx/0/a/b/c",1,true)
        def example = new ExampleClass()
        example.asyncBean=new AsyncBean()
        def asyncBeanField = Mock(AnnotatedField){
            getJavaMember() >> ExampleClass.class.getField('asyncBean')
        }
         def annotatedType = Mock(AnnotatedType){
            getFields() >> new HashSet<>([asyncBeanField])
        }
        def processInjectionTarget = Mock(ProcessInjectionTarget){
            getAnnotatedType() >> annotatedType
            getInjectionTarget() >> Mock(InjectionTarget)
            setInjectionTarget(_) >> {args->
                args[0].inject(example,null)
            }
        }

        when: 'deploying the example bean'
        new HeaderPropagatingExtension().processInjectionTarget(processInjectionTarget)

        then: 'the AsyncInvocationHandler has been used to wrap the existing object in a proxy that creates a child threadContextId'
        AsyncInvocationHandler.setCorrelationStateForTests( BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE)
        example.asyncBean.doStuff()
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.setCurrentThreadCorrelationContext(AsyncBean.getMethod("doStuff"), new Object[0])
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.getCurrentThreadContextId() == 101

    }
}
