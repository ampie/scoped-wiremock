package com.sbg.bdd.wiremock.scoped.integration;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class BaseRuntimeCorrelationState implements RuntimeCorrelationState {
    private static final Logger LOGGER = Logger.getLogger(RuntimeCorrelationState.class.getName());
    //TODO reevaluate if this is still necessary
    private static String wireMockInternalHostName;//the hostname this process uses to talk to WireMock (e.g. Android: 10.0.2.2)
    private Map<InvocationKey, ThreadCorrelationContext> contextsByInvocationKey = new ConcurrentHashMap<>();
    //TODO temp hack for Android. Remove when we support multithreaded correlationContext in our superclass of Android AsyncTask
    private ThreadCorrelationContext rootThreadCorrelationContext;
    private ThreadLocal<ThreadCorrelationContext> currentThreadCorrelationContext = new ThreadLocal<>();
    private String correlationPath;
    private boolean proxyUnmappedEndpoints = false;
    private Integer wireMockPort;
    //The hostname downstream process would use to talk to wireMock
    private String wireMockHost;
    private Map<String, ServiceInvocationCount> allSequenceNumbersInScope = new ConcurrentHashMap<>();

    public BaseRuntimeCorrelationState() {
    }
    @Override
    public void set(String correlationPath, int threadContext, boolean proxyUnmappedEndpoints) {
        this.correlationPath = correlationPath;
        String[] split = correlationPath.split("/");
        this.wireMockPort = Integer.parseInt(split[1]);
        this.proxyUnmappedEndpoints = proxyUnmappedEndpoints;
        this.wireMockHost = split[0];
        currentThreadCorrelationContext.set(new ThreadCorrelationContext(threadContext));
        rootThreadCorrelationContext=currentThreadCorrelationContext.get();
    }

    public String getCorrelationPath() {
        return correlationPath;
    }

    public static void connectToWireMockOn(String wireMockInternalHostName) {
        BaseRuntimeCorrelationState.wireMockInternalHostName = wireMockInternalHostName;
    }

    @Override
    public Collection<ServiceInvocationCount> getServiceInvocationCounts() {
        return new TreeSet(allSequenceNumbersInScope.values());
    }

    @Override
    public URL getWireMockBaseUrl() {
        try {
            return new URL("http://" + (wireMockInternalHostName == null ? wireMockHost : wireMockInternalHostName) + ":" + wireMockPort);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
    //Call this method before a new thread context is started
    public void newChildContext(Method method, Object[] parameters) {
        this.contextsByInvocationKey.put(new InvocationKey(method, parameters), new ThreadCorrelationContext(this.currentThreadCorrelationContext.get()));
    }

    //Call this method once a new thread context is started and running
    @Override
    public void setCurrentThreadCorrelationContext(Method method, Object[] parameters) {
        ThreadCorrelationContext correlationContext = this.contextsByInvocationKey.get(new InvocationKey(method, parameters));
        this.currentThreadCorrelationContext.set(correlationContext);
    }

    //Call this method once a thread context has stopped running
    public void clearCurrentThreadCorrelationContext(Method method, Object[] parameters) {
        this.contextsByInvocationKey.remove(new InvocationKey(method, parameters));
        this.currentThreadCorrelationContext.remove();
    }

    public Integer getNextSequenceNumberFor(String endPointIdentifier) {
        String key = ServiceInvocationCount.keyOf(getCurrentThreadContextId(), endPointIdentifier);
        ServiceInvocationCount serviceInvocationCount = this.allSequenceNumbersInScope.get(key);
        if (serviceInvocationCount == null) {
            this.allSequenceNumbersInScope.put(key, serviceInvocationCount = new ServiceInvocationCount(getCurrentThreadContextId(), endPointIdentifier, 0));
        }
        return serviceInvocationCount.next();
    }


    @Override
    public void clear() {
        wireMockPort = null;
        correlationPath = null;
        allSequenceNumbersInScope.clear();
        proxyUnmappedEndpoints = false;
        currentThreadCorrelationContext.remove();
        contextsByInvocationKey.clear();
    }

    public boolean isSet() {
        return correlationPath != null;
    }

    @Override
    public void initSequenceNumberFor(ServiceInvocationCount serviceInvocationCount) {
        allSequenceNumbersInScope.put(serviceInvocationCount.getKey(),serviceInvocationCount);
    }

    public boolean shouldProxyUnmappedEndpoints() {
        return this.proxyUnmappedEndpoints;
    }

    @Override
    public int getCurrentThreadContextId() {
        if(currentThreadCorrelationContext.get()==null){
            //TODO temp hack for Android.
            return rootThreadCorrelationContext.getContextId();
        }
        return currentThreadCorrelationContext.get().getContextId();
    }


}
