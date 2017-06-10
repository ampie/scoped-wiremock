package com.sbg.bdd.wiremock.scoped.jaxrs;

import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointCategory;
import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URL;
import java.util.Map;

public class DynamicWebTarget implements WebTarget {
    private final EndPointRegistry endpointRegistry;
    private final WireMockCorrelationState currentCorrelationState;
    private WebTarget delegate;
    private final EndPointProperty endPointProperty;
    private final EndPointCategory endPointCategory;
    private Client client;

    public DynamicWebTarget(Client client, EndPointProperty endPointProperty, EndPointCategory endPointCategory) {
        this.client = client;
        this.endpointRegistry = DependencyInjectionAdaptorFactory.getAdaptor().getEndpointRegistry();
        this.endPointProperty = endPointProperty;
        this.endPointCategory = endPointCategory;
        currentCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
    }
    private WebTarget getDelegate() {
        if (delegate == null) {
            try {
                URL url = endpointRegistry.endpointUrlFor(endPointProperty.value());
                if (currentCorrelationState.isSet()) {
                    url = new URL(currentCorrelationState.getWireMockBaseUrl() + url.getFile() + (url.getQuery() == null ? "" : url.getQuery()));
                    delegate = client.target(url.toURI());
                } else {
                    delegate = client.target(url.toURI());
                }
                delegate.register(InboundResponseCorrelationKeyFilter.class);
                delegate.register(OutboundRequestCorrelationKeyFilter.class);

            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return delegate;
    }
    @Override
    public Invocation.Builder request() {
        try {
            if (endPointCategory != null && currentCorrelationState.isSet() ) {
                return getDelegate().request().header(HeaderName.ofTheEndpointCategory(), endPointCategory.value());
            }
            return getDelegate().request();
        } finally {
            delegate=null;
        }
    }
    @Override
    public URI getUri() {
        return getDelegate().getUri();
    }

    @Override
    public UriBuilder getUriBuilder() {
        return getDelegate().getUriBuilder();
    }

    @Override
    public WebTarget path(String path) {
        delegate = getDelegate().path(path);
        return this;
    }

    @Override
    public WebTarget resolveTemplate(String name, Object value) {
        delegate = getDelegate().resolveTemplate(name, value);
        return this;

    }

    @Override
    public WebTarget resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
        delegate = getDelegate().resolveTemplate(name, value, encodeSlashInPath);
        return this;
    }

    @Override
    public WebTarget resolveTemplateFromEncoded(String name, Object value) {
        delegate = getDelegate().resolveTemplateFromEncoded(name, value);
        return this;
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues) {
        delegate = getDelegate().resolveTemplates(templateValues);
        return this;
    }

    @Override
    public WebTarget resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) {
        delegate = getDelegate().resolveTemplates(templateValues, encodeSlashInPath);
        return this;
    }

    @Override
    public WebTarget resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
        delegate = getDelegate().resolveTemplatesFromEncoded(templateValues);
        return this;
    }

    @Override
    public WebTarget matrixParam(String name, Object... values) {
        delegate = getDelegate().matrixParam(name, values);
        return this;
    }

    @Override
    public WebTarget queryParam(String name, Object... values) {
        delegate = getDelegate().queryParam(name, values);
        return this;
    }



    @Override
    public Invocation.Builder request(String... acceptedResponseTypes) {
        return request().accept(acceptedResponseTypes);
    }

    @Override
    public Invocation.Builder request(MediaType... acceptedResponseTypes) {
        return request().accept(acceptedResponseTypes);
    }

    @Override
    public Configuration getConfiguration() {
        return getDelegate().getConfiguration();
    }

    @Override
    public WebTarget property(String name, Object value) {
        delegate = getDelegate().property(name, value);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass) {
        delegate = getDelegate().register(componentClass);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, int priority) {
        delegate = getDelegate().register(componentClass, priority);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, Class<?>... contracts) {
        delegate = getDelegate().register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTarget register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        delegate = getDelegate().register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTarget register(Object component) {
        delegate = getDelegate().register(component);
        return this;
    }

    @Override
    public WebTarget register(Object component, int priority) {
        delegate = getDelegate().register(component, priority);
        return this;
    }

    @Override
    public WebTarget register(Object component, Class<?>... contracts) {
        delegate = getDelegate().register(component, contracts);
        return this;
    }

    @Override
    public WebTarget register(Object component, Map<Class<?>, Integer> contracts) {
        delegate = getDelegate().register(component, contracts);
        return this;
    }


}
