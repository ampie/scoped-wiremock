package com.sbg.bdd.wiremock.scoped.cdi.internal

import com.sbg.bdd.wiremock.scoped.integration.BaseRuntimeCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import org.jboss.security.SecurityContextAssociation
import org.jboss.security.plugins.JBossSecurityContext
import spock.lang.Specification

class WhenRetrievingTheCurrentCorrelationStateInJBoss extends Specification{
    def 'it should retrieve the RuntimeCorrelationState on the current security context'(){
        given: 'The JBossDependencyInjectorAdaptor is used as the DependencyInjectionAdaptor to use in JBoss'
        JBossDependencyInjectorAdaptor.clearInWildfly()
        DependencyInjectionAdaptorFactory.useAdaptor(new JBossDependencyInjectorAdaptor())
        and: 'A JBoss SecurityContext that already has a RuntimeCorrelationState'
        def securityContext = new JBossSecurityContext('myContext')
        def correlationState = new BaseRuntimeCorrelationState()
        securityContext.data['correlationState'] = correlationState
        SecurityContextAssociation.setSecurityContext(securityContext)
        expect: 'the currentCorrelationState to be the same one previously created'
        DependencyInjectionAdaptorFactory.currentCorrelationState==correlationState
    }
    def 'it should create a newRuntimeCorrelationState on the current security context if none was set'(){
        given: 'The JBossDependencyInjectorAdaptor is used as the DependencyInjectionAdaptor to use in JBoss'
        DependencyInjectionAdaptorFactory.useAdaptor(new JBossDependencyInjectorAdaptor())
        JBossDependencyInjectorAdaptor.clearInWildfly()
        and: 'A JBoss SecurityContext that does not have a RuntimeCorrelationState'
        def securityContext = new JBossSecurityContext('myContext')
        SecurityContextAssociation.setSecurityContext(securityContext)
        expect: 'the currentCorrelationState to be the same one previously created'
        DependencyInjectionAdaptorFactory.currentCorrelationState!=null
    }
}
