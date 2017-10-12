package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScopeUpdatingResponseTransformer extends ResponseTransformer {
    @Override
    public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
        ScopedAdmin admin = ScopeExtensions.getCurrentAdmin();
        CorrelationStateSynchronizer synchronizer = new CorrelationStateSynchronizer(admin, response.getHeaders());
        if (synchronizer.canProcess()) {
            synchronizer.synchronize();
        } else {
            synchronizer = new CorrelationStateSynchronizer(admin, request.getHeaders());
            if(synchronizer.canProcess()){
                synchronizer.synchronize();
                response=synchronizer.synchronize(response);

            }
        }
        return response;
    }

    @Override
    public boolean applyGlobally() {
        return true;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    private class CorrelationStateSynchronizer {
        private HttpHeader correlationKey;
        private HttpHeader invocationCounts;
        private CorrelationState correlationState;
        private boolean canProcess;

        public CorrelationStateSynchronizer(ScopedAdmin admin, HttpHeaders headers) {
            canProcess = false;
            correlationKey = headers.getHeader(HeaderName.ofTheCorrelationKey());
            if (correlationKey != null && correlationKey.isPresent()) {
                correlationState = admin.getCorrelatedScope(correlationKey.firstValue());
                invocationCounts = headers.getHeader(HeaderName.ofTheServiceInvocationCount());
                if (invocationCounts.isPresent()) {
                    if (correlationState != null) {
                        canProcess = true;
                    }
                }
            }
        }

        public boolean canProcess() {
            return canProcess;
        }

        public void synchronize() {
            List<String> invocationCountValues = extractAndFlattenInvocationCounts();
            for (String s : invocationCountValues) {
                String[] split = s.split("\\|");
                correlationState.getServiceInvocationCounts().put(split[0], Integer.valueOf(split[1]));
            }
        }

        public Response synchronize(Response response) {
            List<String> invocationCountValues = extractAndFlattenInvocationCounts();
            HttpHeaders headers = response.getHeaders();
            if(!headers.getHeader(HeaderName.ofTheCorrelationKey()).isPresent()){
                headers = headers.plus(correlationKey);
            }
            headers = headers.plus(new HttpHeader(HeaderName.ofTheServiceInvocationCount(),invocationCountValues));
            return Response.Builder.like(response).headers(headers).build();
        }

        public List<String> extractAndFlattenInvocationCounts() {
            List<String> invocationCountValues;
            if(invocationCounts.values().size()==1 && invocationCounts.values().get(0).indexOf(',')>0){
                //may have been flattened like it seems Apache CXF likes doing
                invocationCountValues= Arrays.asList(invocationCounts.values().get(0).split("\\,"));
            }else{
                invocationCountValues= invocationCounts.values();
            }
            return invocationCountValues;
        }
    }
}
