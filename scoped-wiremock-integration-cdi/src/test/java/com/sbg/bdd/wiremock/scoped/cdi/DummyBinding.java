package com.sbg.bdd.wiremock.scoped.cdi;

import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.handler.Handler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DummyBinding implements BindingProvider,TheWebService {


    private Map<String, Object> requestContext=new HashMap<>();
    private Binding binding=new MyBinding();

    @Override
    public void doStoff() {

    }

    @Override
    public Map<String, Object> getRequestContext() {
        return requestContext;
    }

    @Override
    public Map<String, Object> getResponseContext() {
        return null;
    }

    @Override
    public Binding getBinding() {
        return binding;
    }

    @Override
    public EndpointReference getEndpointReference() {
        return null;
    }

    @Override
    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
        return null;
    }

    private static class MyBinding implements Binding {
        private List<Handler> handlerChain=new ArrayList<>();

        @Override
        public List<Handler> getHandlerChain() {
            return handlerChain;
        }

        @Override
        public void setHandlerChain(List<Handler> handlerChain) {
            this.handlerChain = handlerChain;
        }

        @Override
        public String getBindingID() {
            return null;
        }
    }
}
