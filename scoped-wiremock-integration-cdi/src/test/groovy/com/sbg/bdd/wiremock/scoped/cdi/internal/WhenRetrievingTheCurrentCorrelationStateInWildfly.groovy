package com.sbg.bdd.wiremock.scoped.cdi.internal

import com.sbg.bdd.wiremock.scoped.cdi.PrincipalInterface1
import com.sbg.bdd.wiremock.scoped.cdi.PrincipalInterface2
import com.sbg.bdd.wiremock.scoped.cdi.UnproxiablePrincipal
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import org.jboss.security.SecurityContextAssociation
import org.jboss.security.plugins.JBossSecurityContext
import spock.lang.Specification

import java.security.Principal

class WhenRetrievingTheCurrentCorrelationStateInWildfly extends Specification{
    def 'it should make the RuntimeCorrelationState available on the AnonymousCorrelatedPrincipal when no UserPrincipal is available'(){
        given: 'The JBossDependencyInjectorAdaptor is used as the DependencyInjectionAdaptor to use in Wildfly'
        JBossDependencyInjectorAdaptor.clearInWildfly()
        DependencyInjectionAdaptorFactory.useAdaptor(new JBossDependencyInjectorAdaptor())
        and: 'A Wildfly SecurityContext that does not have a RuntimeCorrelationState or a Principal'
        def securityContext = new JBossSecurityContext('myContext')
        SecurityContextAssociation.setSecurityContext(securityContext)
        when: 'I retrieve the the currentCorrelationState'
        def correlationState = DependencyInjectionAdaptorFactory.currentCorrelationState
        then: 'it should have a value'
        correlationState !=null
        and: 'the current UserPrincipal should be the AnonymousCorrelatedPrincipal containing the currentCorrelationState'
        def userPrincipal = SecurityContextAssociation.securityContext.util.userPrincipal
        userPrincipal instanceof AnonymousCorrelatedPrincipal
        userPrincipal.correlationState == correlationState
    }
    def 'it should make the RuntimeCorrelationState available on a CorrelatedPrincipal proxy when a UserPrincipal is available'(){
        given: 'The JBossDependencyInjectorAdaptor is used as the DependencyInjectionAdaptor to use in Wildfly'
        JBossDependencyInjectorAdaptor.clearInWildfly()
        DependencyInjectionAdaptorFactory.useAdaptor(new JBossDependencyInjectorAdaptor())
        and: 'A Wildfly SecurityContext that has a Principal that cannot be proxied'
        def securityContext = new JBossSecurityContext('myContext')
        SecurityContextAssociation.setSecurityContext(securityContext)
        securityContext.util.createSubjectInfo(new UnproxiablePrincipal(),null,null)
        when: 'I retrieve the the currentCorrelationState'
        def correlationState = DependencyInjectionAdaptorFactory.currentCorrelationState
        then: 'it should have a value'
        correlationState !=null
        and: 'the current UserPrincipal should be the proxied CorrelatedPrincipal containing the currentCorrelationState'
        def userPrincipal = SecurityContextAssociation.securityContext.util.userPrincipal
        userPrincipal instanceof CorrelatedPrincipal
        userPrincipal.correlationState == correlationState
        and: 'the proxied UserPrincipal should implement all the interfaces implemented by the unproxiable principal class'
        userPrincipal instanceof PrincipalInterface1
        userPrincipal instanceof PrincipalInterface2
        userPrincipal instanceof Principal

    }

}
