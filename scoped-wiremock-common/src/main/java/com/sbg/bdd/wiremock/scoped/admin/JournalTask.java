package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.admin.model.ExchangeJournalRequest;
import com.sbg.bdd.wiremock.scoped.admin.model.JournalMode;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;

public class JournalTask extends  ScopeAdminTask {
    public JournalTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
        ExchangeJournalRequest journalRequest = Json.read(request.getBodyAsString(), ExchangeJournalRequest.class);
        ResourceContainer resourceRoot = super.admin.getResourceRoot(journalRequest.getResourceRoot());
        ResourceContainer targetDir = resourceRoot.resolvePotentialContainer(journalRequest.getPath());
        if(journalRequest.getMode() == JournalMode.RECORD) {
            super.admin.saveRecordingsForRequestPattern(journalRequest.getRequestPattern(), targetDir);
        }else if(journalRequest.getMode() == JournalMode.PLAYBACK){
            super.admin.serveRecordedMappingsAt(targetDir,journalRequest.getRequestPattern(),journalRequest.getPriority());
        }
        return responseDefinition()
                .withStatus(HTTP_NO_CONTENT)
                .withHeader("Content-Type", "application/json")
                .build();
    }
}
