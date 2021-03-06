package com.sbg.bdd.wiremock.scoped.cdi.internal;


import com.sbg.domain.common.annotations.EndpointInfo;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

class HeaderPropagatingInjectionTargetDecorator<X> implements InjectionTarget<X> {
    private static final Logger LOGGER = Logger.getLogger(HeaderPropagatingInjectionTargetDecorator.class.getName());
    private final InjectionTarget<X> it;
    private final Set<Field> webServiceRefs;
    private Set<Field> asyncBeans;

    public HeaderPropagatingInjectionTargetDecorator(InjectionTarget<X> it, Set<Field> webServiceRefs, Set<Field> asyncBeans) {
        this.it = it;
        this.webServiceRefs = webServiceRefs;
        this.asyncBeans = asyncBeans;
    }

    @Override
    public void inject(X instance, CreationalContext<X> ctx) {
        it.inject(instance, ctx);
        for (Field webServiceRef : webServiceRefs) {
            try {
                Object ref = webServiceRef.get(instance);
                if (ref == null) {
                    //Fallback - should generally not happen.
                    // Leave it here because when it fails it provides some useful diagnostic information such as when the WSDL file is not found
                    try {
                        WebServiceRef webServiceRefAnnotation = webServiceRef.getAnnotation(WebServiceRef.class);
                        //TODO could derive the type from the WSDL...? too much work
                        Class<?> serviceClass = webServiceRefAnnotation.value();
                        Service service = (Service) serviceClass.newInstance();
                        ref = service.getPort(webServiceRef.getType());
                    } catch (ReflectiveOperationException e) {
                        LOGGER.log(Level.WARNING, "Could not create service:", e);
                    }
                }
                if (ref instanceof BindingProvider) {
                    BindingProvider bp = (BindingProvider) ref;
                    wrapReference(instance, webServiceRef, bp);
                }
            } catch (IllegalAccessException | IllegalArgumentException | IllegalStateException e) {
                LOGGER.log(Level.WARNING, "Could not inject:", e);
            }
        }
        for (Field asyncBean : asyncBeans) {
            try {
                Object asyncObject = asyncBean.get(instance);
                asyncBean.set(instance, AsyncHeaderPropagatingHandler.create(asyncBean,asyncObject));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not inject:", e);
            }
        }
    }

    private void wrapReference(X instance, Field webServiceRef, BindingProvider bp) throws IllegalAccessException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        EndpointInfo mep = webServiceRef.getAnnotation(EndpointInfo.class);
        WebServiceHeaderPropagatingHandler ih = new WebServiceHeaderPropagatingHandler(bp,mep);
        webServiceRef.set(instance, Proxy.newProxyInstance(cl, getInterfaces(webServiceRef), ih));
    }

    private Class<?>[] getInterfaces(Field webServiceRef) {
        return new Class<?>[]{BindingProvider.class, webServiceRef.getType()};
    }

    @Override
    public void postConstruct(X instance) {
        it.postConstruct(instance);
    }

    @Override
    public void preDestroy(X instance) {
        it.dispose(instance);
    }


    @Override
    public void dispose(X instance) {
        it.dispose(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return it.getInjectionPoints();
    }

    @Override
    public X produce(CreationalContext<X> ctx) {
        return it.produce(ctx);
    }

}
