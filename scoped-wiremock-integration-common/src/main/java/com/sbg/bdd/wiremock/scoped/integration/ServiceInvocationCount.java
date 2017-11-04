package com.sbg.bdd.wiremock.scoped.integration;

public class ServiceInvocationCount implements Comparable<ServiceInvocationCount> {
    private int threadContextId;
    private String endpointIdentifier;
    private int count;

    public ServiceInvocationCount(int threadContextId, String endpointIdentifier, int count) {
        this.threadContextId = threadContextId;
        this.endpointIdentifier = endpointIdentifier;
        this.count = count;
    }
    public ServiceInvocationCount(String string) {
        String[] split = string.split("\\|");
        this.threadContextId = Integer.parseInt(split[0]);
        this.endpointIdentifier = split[1];
        this.count = Integer.parseInt(split[2]);
    }
    public String getKey(){
        return keyOf(threadContextId,endpointIdentifier);
    }

    public static String keyOf(int threadContextId, String endpointIdentifier) {
        return threadContextId + endpointIdentifier;
    }

    public int getThreadContextId() {
        return threadContextId;
    }

    public String getEndpointIdentifier() {
        return endpointIdentifier;
    }

    public int getCount() {
        return count;
    }
    public int next(){
        return ++count;
    }
    public String toString(){
        return threadContextId + "|" + endpointIdentifier + "|" + count;
    }

    @Override
    public int compareTo(ServiceInvocationCount serviceInvocationCount) {
        if(serviceInvocationCount.threadContextId==threadContextId){
            return endpointIdentifier.compareTo(serviceInvocationCount.endpointIdentifier);
        }
        return threadContextId-serviceInvocationCount.threadContextId;
    }
}
