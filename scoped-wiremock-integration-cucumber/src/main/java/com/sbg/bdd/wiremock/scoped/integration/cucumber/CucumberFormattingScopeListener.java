package com.sbg.bdd.wiremock.scoped.integration.cucumber;

import com.github.tomakehurst.wiremock.common.Json;
import com.sbg.bdd.cucumber.common.MapParser;
import com.sbg.bdd.cucumber.common.ScreenPlayFormatter;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;
import com.sbg.bdd.wiremock.scoped.server.ScopeListener;
import gherkin.deps.net.iharder.Base64;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CucumberFormattingScopeListener implements ScopeListener {
    private ScreenPlayFormatter reporter;
    private MapParser parser;
    private ScopedAdmin admin;
    private StringWriter out;

    public CucumberFormattingScopeListener() {
    }

    @Override
    public void setScopedAdmin(ScopedAdmin admin) {
        this.admin = admin;

    }

    @Override
    public void scopeStarted(CorrelationState knownScope) {
        if (knownScope.getCorrelationPath().split("\\/").length == 4) {//ip/port/name/runid
            out = new StringWriter();
            reporter = new ScreenPlayFormatter(out);
            this.parser = new MapParser(reporter, reporter);
        }
        Map<String, Object> payload = knownScope.getPayload();
        if (payload != null) {
            if ("feature".equals(payload.get("method"))) {
                parser.replayFeature(payload);
            } else if ("featureElement".equals(payload.get("method"))) {
                parser.replayFeatureElement(payload);
            }
        }
    }

    @Override
    public void scopeStopped(CorrelationState state) {
        if (state.getCorrelationPath().split("\\/").length == 4) {//ip/port/name/runid
            reporter.done();
            admin.getResourceRoot("outputResourceRoot").resolvePotential("cucumber.json").write(out.toString().getBytes());
        }
    }

    @Override
    public void stepStarted(CorrelationState state) {
        try {
            Map<String, Object> payload = state.getPayload();
            if (payload != null && !payload.isEmpty()) {
                if ("childStepAndMatch".equals(payload.get("method"))) {
                    parser.replayChildStepAndMatch(payload);
                } else if ("stepAndMatch".equals(payload.get("method"))) {
                    parser.replayStepAndMatch(payload);
                }
            }
        } catch (Exception e) {
            //Be proactive rather
            e.printStackTrace();
        }
    }

    @Override
    public void stepCompleted(CorrelationState state) {
        try {
            Map<String, Object> payload = state.getPayload();
            if (payload != null && !payload.isEmpty()) {
                List<Map<String, Object>> embeddings = (List<Map<String, Object>>) payload.get("embeddings");
                if (embeddings != null) {
                    for (Map<String, Object> embedding : embeddings) {
                        parser.replayEmbedding(embedding);
                    }
                }
                //TODO find Mappings that were made during the step - maintain a map of them in the currentScopestate
                List<RecordedExchange> exchanges = admin.findExchangesAgainstStep(state.getCorrelationPath(), state.getCurrentStep());
                if (exchanges.size() > 0) {
                    Map<String, Object> embedding = new HashMap<>();
                    embedding.put("mime_type", "application/json");
                    embedding.put("data", Base64.encodeBytes(Json.write(exchanges).getBytes()));
                    parser.replayEmbedding(embedding);
                }
                if ("childResult".equals(payload.get("method"))) {
                    parser.replayChildResult(payload);
                } else if ("result".equals(payload.get("method"))) {
                    parser.replayResult(payload);
                }
            }
        } catch (Exception e) {
            //be proactive
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "CucumberFormattingScopeListener";
    }
}
