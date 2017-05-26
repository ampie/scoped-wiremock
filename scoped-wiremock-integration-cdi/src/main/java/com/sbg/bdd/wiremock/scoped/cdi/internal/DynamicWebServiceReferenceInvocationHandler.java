package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;
import com.sbg.bdd.wiremock.scoped.jaxws.OutboundCorrelationPathSOAPHandler;

import javax.jws.WebMethod;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

public class DynamicWebServiceReferenceInvocationHandler implements InvocationHandler {
    private static final Logger LOGGER = Logger.getLogger(DynamicWebServiceReferenceInvocationHandler.class.getName());
    private BindingProvider delegate;
    private final EndPointRegistry endpointRegistry;
    private final EndPointProperty endPointProperty;

    public DynamicWebServiceReferenceInvocationHandler(BindingProvider delegate, EndPointRegistry endpointRegistry, EndPointProperty endPointProperty) {
        this.endpointRegistry=endpointRegistry;
        this.endPointProperty=endPointProperty;
        this.delegate=delegate;
        attachInterceptor(delegate);
    }
    private boolean isInterceptorAdded(List<Handler> handlerChain) {
        boolean found = false;
        for (Handler handler : handlerChain) {
            if (handler instanceof OutboundCorrelationPathSOAPHandler) {
                found = true;
                break;
            }
        }
        return found;
    }
    
    private void attachInterceptor(BindingProvider bp) {
        List<Handler> handlerChain = bp.getBinding().getHandlerChain();
        if (!isInterceptorAdded(handlerChain)) {
            OutboundCorrelationPathSOAPHandler i = new OutboundCorrelationPathSOAPHandler();
            handlerChain.add(i);
            bp.getBinding().setHandlerChain(handlerChain);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if(method.isAnnotationPresent(WebMethod.class) && !isProbablyAlreadyMocked()){
                delegate.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,endpointRegistry.endpointUrlFor(endPointProperty.value()));
            }
            return method.invoke(delegate,args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private boolean isProbablyAlreadyMocked() {
        //TODO just keep an eye on this
        String endpoint = (String) delegate.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        if(endpoint !=null && endpoint.startsWith("http://localhost")){
            return true;
        }
        return false;
    }

}
