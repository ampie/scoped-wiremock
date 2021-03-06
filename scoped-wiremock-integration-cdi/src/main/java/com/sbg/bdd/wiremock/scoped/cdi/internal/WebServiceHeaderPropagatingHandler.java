package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.domain.common.annotations.EndpointInfo;
import com.sbg.bdd.wiremock.scoped.integration.*;
import com.sbg.bdd.wiremock.scoped.jaxws.OutboundCorrelationPathSOAPHandler;

import javax.jws.WebMethod;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class WebServiceHeaderPropagatingHandler implements InvocationHandler {
    private static final Logger LOGGER = Logger.getLogger(WebServiceHeaderPropagatingHandler.class.getName());
    private BindingProvider delegate;
    private EndpointRegistry endpointRegistry;
    private EndpointInfo endpointInfo;

    public WebServiceHeaderPropagatingHandler(BindingProvider delegate, EndpointInfo endpointInfo) {
        this.endpointInfo = endpointInfo;
        this.delegate = delegate;
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
            if (method.isAnnotationPresent(WebMethod.class) && !isProbablyAlreadyMocked()) {
                URL originalUrl = getEndpointRegistry().endpointUrlFor(endpointInfo.propertyName());
                URL urlToUse = getUrlToUse(originalUrl);
                if(endpointInfo.categories()!=null && endpointInfo.categories().length> 0) {
                    delegate.getRequestContext().put(HeaderName.ofTheEndpointCategory(), Arrays.asList(endpointInfo.categories()));
                }
                delegate.getRequestContext().put(HeaderName.ofTheOriginalUrl(), originalUrl);
                delegate.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, urlToUse.toExternalForm());
            }
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private URL getUrlToUse(URL originalUrl) throws MalformedURLException {
        RuntimeCorrelationState currentCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
        if (currentCorrelationState.isSet()) {
            return URLHelper.replaceBaseUrl(originalUrl, currentCorrelationState.getWireMockBaseUrl());
        }
        return originalUrl;
    }

    private EndpointRegistry getEndpointRegistry() {
        if (endpointRegistry == null) {
            endpointRegistry = DependencyInjectionAdaptorFactory.getAdaptor().getEndpointRegistry();
        }
        return endpointRegistry;
    }

    private boolean isProbablyAlreadyMocked() {
        //TODO just keep an eye on this and then remove it. The assumption does not hold true anymore
        String endpoint = (String) delegate.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        if (endpoint != null && endpoint.startsWith("http://localhost")) {
//            return true;
        }
        return false;
    }

}
