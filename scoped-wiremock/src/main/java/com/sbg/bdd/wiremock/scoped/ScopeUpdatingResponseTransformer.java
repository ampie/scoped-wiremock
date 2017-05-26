package com.sbg.bdd.wiremock.scoped;

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
        private ScopedAdmin admin;
        private HttpHeaders headers;
        private HttpHeader correlationKey;
        private HttpHeader invocationCounts;
        private CorrelationState correlationState;
        private boolean canProcess;

        public CorrelationStateSynchronizer(ScopedAdmin admin, HttpHeaders headers) {
            this.admin = admin;
            this.headers = headers;
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
            for (String s : invocationCounts.values()) {
                String[] split = s.split("\\|");
                correlationState.getServiceInvocationCounts().put(split[0], Integer.valueOf(split[1]));
            }
        }
    }
}
