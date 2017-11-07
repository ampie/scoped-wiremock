package com.sbg.bdd.wiremock.scoped.cdi.internal

import com.sbg.bdd.wiremock.scoped.cdi.AsyncBean
import com.sbg.bdd.wiremock.scoped.cdi.ExampleClass
import com.sbg.bdd.wiremock.scoped.integration.*
import org.jboss.security.SecurityContextAssociation
import org.jboss.security.plugins.JBossSecurityContext
import spock.lang.Specification

import javax.enterprise.inject.spi.AnnotatedField
import javax.enterprise.inject.spi.AnnotatedType
import javax.enterprise.inject.spi.InjectionTarget
import javax.enterprise.inject.spi.ProcessInjectionTarget

class WhenDeployingAClassWithAsynchronousReferences extends Specification{

    def 'references to async beans with the PropagatesHeaders annotation should be wrapped in a proxy ' (){
        //TODO is there a more direct way to test this? This sounds like a WhenInvoking.... test
        given:
        SecurityContextAssociation.setSecurityContext(new JBossSecurityContext('asdf'))
        DependencyInjectionAdaptorFactory.useAdaptor(new JBossDependencyInjectorAdaptor())
        DependencyInjectionAdaptorFactory.currentCorrelationState.set("localhost/123/runx/0/a/b/c",1,true)
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

        then: 'the AsyncHeaderPropagatingHandler has been used to wrap the existing object in a proxy that creates a child threadContextId'
        example.asyncBean.doStuff()
        DependencyInjectionAdaptorFactory.currentCorrelationState.setCurrentThreadCorrelationContext(AsyncBean.getMethod("doStuff"), new Object[0])
        DependencyInjectionAdaptorFactory.currentCorrelationState.getCurrentThreadContextId() == 101

    }
}
