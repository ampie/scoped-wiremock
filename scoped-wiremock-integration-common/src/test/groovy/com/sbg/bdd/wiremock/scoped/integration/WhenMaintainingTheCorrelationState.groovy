package com.sbg.bdd.wiremock.scoped.integration

import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class WhenMaintainingTheCorrelationState extends Specification {
    def 'it should extract the WireMock host and port from the correlation path'() {
        given:
        def state = new BaseRuntimeCorrelationState()

        when:

        state.set( 'some.host/9090/scope1/scope1_1',1,true)

        then:
        state.correlationPath == 'some.host/9090/scope1/scope1_1'
        state.wireMockBaseUrl == new URL('http://some.host:9090')
    }

    def 'it should keep track of the number of times a certain service was called'() {
        given:
        def state = new BaseRuntimeCorrelationState()
        state.set('some.host/9090/scope1/scope1_1',3,true)
        state.initSequenceNumberFor(new ServiceInvocationCount('3|my.endpoint|6'))

        expect:
        state.getNextSequenceNumberFor('my.endpoint') == 7
        state.getNextSequenceNumberFor('my.endpoint') == 8
    }
    def 'it should keep track of the number of times a certain service was called from different threads'() {
        given:
        def state = new BaseRuntimeCorrelationState()
        state.set('some.host/9090/scope1/scope1_1',1,true)
        state.initSequenceNumberFor(new ServiceInvocationCount('1|my.endpoint|1'))

        when:
        def async = new ExampleAsync(state);
        def future1 = async.doAsync('hello', 1)
        def future2 = async.doAsync('hello', 2)
        def future3 = async.doAsync('hello', 3)
        future3.get()
        future2.get()
        future1.get()
        def counts = state.serviceInvocationCounts
        then:
        counts.size()==4
        counts.any {it.count == 1 && it.endpointIdentifier=='my.endpoint' && it.threadContextId == 1}
        counts.any {it.count == 1 && it.endpointIdentifier=='my.endpoint' && it.threadContextId == 101}
        counts.any {it.count == 2 && it.endpointIdentifier=='my.endpoint' && it.threadContextId == 102}
        counts.any {it.count == 3 && it.endpointIdentifier=='my.endpoint' && it.threadContextId == 103}

    }
    class ExampleAsync{
        BaseRuntimeCorrelationState correlationState;
        ExampleAsync(BaseRuntimeCorrelationState correlationState) {
            this.correlationState = correlationState
        }
        public Future<String> doAsync(String message, int number){
            def method = ExampleAsync.getMethod('doAsync', String, int)
            correlationState.newChildContext(method,[message, number].toArray())
            def future = new CompletableFuture<String>()

            new Thread(){
                @Override
                void run() {
                    correlationState.setCurrentThreadCorrelationContext(method,[message, number].toArray())
                    println 'dostufff to slow down the test a bit...'
                    for (int i = 0; i < number; i++) {
                        correlationState.getNextSequenceNumberFor('my.endpoint')
                    }
                    future.complete(message + number)
                    correlationState.clearCurrentThreadCorrelationContext(method,[message, number].toArray())
                }
            }.start()
            return future
        }
    }
}
