package com.sbg.bdd.wiremock.scoped.integration.cucumber;

import com.github.tomakehurst.wiremock.common.Json;
import com.sbg.bdd.cucumber.screenplay.core.formatter.ScreenPlayFormatter;
import com.sbg.bdd.cucumber.screenplay.scoped.plugin.MapParser;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;
import com.sbg.bdd.wiremock.scoped.server.ScopeListener;
import gherkin.deps.net.iharder.Base64;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CucumberFormattingScopeListener implements ScopeListener {
    private MapParser parser;
    private ScopedAdmin admin;

    public CucumberFormattingScopeListener() {
        try {
            Appendable file = new FileWriter(new File("./cucumber.json"));
            ScreenPlayFormatter reporter = new ScreenPlayFormatter(file);
            this.parser = new MapParser(reporter, reporter);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setAdmin(ScopedAdmin admin) {
        this.admin = admin;
    }

    @Override
    public void scopeStarted(CorrelationState knownScope) {
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
    }

    @Override
    public void stepStarted(CorrelationState state) {
        Map<String, Object> payload = state.getPayload();
        if (payload != null) {
            if ("childStepAndMatch".equals(payload.get("method"))) {
                parser.replayChildStepAndMatch(payload);
            } else if ("stepAndMatch".equals(payload.get("method"))) {
                parser.replayStepAndMatch(payload);
            }
        }
    }

    @Override
    public void stepCompleted(CorrelationState state) {
        Map<String, Object> payload = state.getPayload();
        if (payload != null) {
            List<Map<String,Object>> embeddings = (List<Map<String, Object>>) payload.get("embeddings");
            for (Map<String, Object> embedding : embeddings) {
                parser.replayEmbedding(embedding);
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
    }

    @Override
    public String getName() {
        return "CucumberFormattingScopeListener";
    }
}
