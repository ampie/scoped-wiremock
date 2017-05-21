package com.github.ampie.wiremock;


import com.github.ampie.wiremock.admin.CorrelationState;
import com.github.tomakehurst.wiremock.client.VerificationException;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class WhenWorkingWithVerificationScopes extends ScopedWireMockTest {

    @Test
    public void shouldJoinAKnownScope() throws IOException {
        //When
        CorrelationState resultScope = getWireMock().joinCorrelatedScope("/my_root_scope");
        //Then
        assertThat(resultScope.getCorrelationPath(), is(equalTo("/my_root_scope")));
        CorrelationState responseToGet = getWireMock().getCorrelatedScope("/my_root_scope");
        assertThat(responseToGet.getCorrelationPath(), is(equalTo("/my_root_scope")));
    }
    @Test
    public void shouldStartANestedScope() throws IOException {
        //Given
        shouldJoinAKnownScope();
        //When
        CorrelationState resultScope = getWireMock().startNewCorrelatedScope("/my_root_scope");
        //Then
        assertThat(resultScope.getCorrelationPath(), is(equalTo("/my_root_scope/1")));
        CorrelationState responseToGet = getWireMock().getCorrelatedScope("/my_root_scope/1");
        assertThat(responseToGet.getCorrelationPath(), is(equalTo("/my_root_scope/1")));
    }
    @Test
    public void shouldKeepTrackOfTheNumerOfServiceInvocationsByScope() throws IOException {
        //Given
        shouldStartANestedScope();
        CorrelationState nestedScope = new CorrelationState("/my_root_scope/1");
        nestedScope.getServiceInvocationCounts().put("service1", 2);
        nestedScope.getServiceInvocationCounts().put("service2", 1);
        //When
        getWireMock().syncCorrelatedScope(nestedScope);
        //Then
        CorrelationState responseToGet = getWireMock().getCorrelatedScope("/my_root_scope/1");
        assertThat(responseToGet.getServiceInvocationCounts().get("service1"), is(equalTo(2)));
        assertThat(responseToGet.getServiceInvocationCounts().get("service2"), is(equalTo(1)));
    }
    @Test
    public void shouldCompleteAScope() throws IOException {
        //Given
        shouldJoinAKnownScope();
        //When
        List<String> removedScopes = getWireMock().stopCorrelatedScope("/my_root_scope");
        //Then
        assertThat(removedScopes.size(), is(equalTo(1)));
        assertThat(removedScopes.get(0), is(equalTo("/my_root_scope")));
        try {
            if(getWireMock().getCorrelatedScope("/my_root_scope") !=null) {
                fail("Scope should not exist any more!");
            }
        } catch (VerificationException e) {
        }
    }
    @Test
    public void shouldCompleteANestedScopeWhenCompleteTheParentScope() throws IOException {
        //Given
        shouldStartANestedScope();
        CorrelationState knownScope = new CorrelationState("/my_root_scope");
        //When
        List<String> removedScopes = getWireMock().stopCorrelatedScope("/my_root_scope");
        //Then
        assertThat(removedScopes.size(), is(equalTo(2)));
        assertThat(removedScopes.get(0), is(equalTo("/my_root_scope")));
        try {
            if(getWireMock().getCorrelatedScope("/my_root_scope/1")!=null) {
                fail("Scope should not exist any more!");
            }
        } catch (VerificationException e) {
        }
    }


}
