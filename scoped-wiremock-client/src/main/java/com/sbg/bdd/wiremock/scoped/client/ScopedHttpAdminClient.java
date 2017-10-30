package com.sbg.bdd.wiremock.scoped.client;

import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.github.tomakehurst.wiremock.admin.AdminRoutes;
import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.extension.AdminApiExtension;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.admin.*;
import com.sbg.bdd.wiremock.scoped.admin.model.*;
import com.sbg.bdd.wiremock.scoped.common.HasBaseUrl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScopedHttpAdminClient extends OkHttpAdminClient implements ScopedAdmin, HasBaseUrl {
    private static final String ADMIN_URL_PREFIX = "%s://%s:%d%s/__admin";
    private final AdminRoutes scopedAdminRoutes;
    private final String scheme;
    private final String host;
    private final int port;
    private final String urlPathPrefix;
    private final String hostHeader;
    private Map<String, ResourceContainer> resourceRoots = new HashMap<>();


    public ScopedHttpAdminClient(String scheme, String host, int port) {
        this(scheme, host, port, "");
    }

    public ScopedHttpAdminClient(String host, int port, String urlPathPrefix) {
        this("http", host, port, urlPathPrefix);
    }

    public ScopedHttpAdminClient(String scheme, String host, int port, String urlPathPrefix) {
        this(scheme, host, port, urlPathPrefix, null, null, 0);
    }

    public ScopedHttpAdminClient(String scheme, String host, int port, String urlPathPrefix, String hostHeader, String proxyHost,
                                 int proxyPort) {
        super(scheme, host, port, urlPathPrefix, hostHeader, proxyHost, proxyPort);
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.urlPathPrefix = urlPathPrefix;
        this.hostHeader = hostHeader;
        this.scopedAdminRoutes = AdminRoutes.defaultsPlus(Arrays.<AdminApiExtension>asList(new ScopeExtensionsOnClient()));
    }

    public ScopedHttpAdminClient(String host, int port) {
        this(host, port, "");
    }

    @Override
    public void register(ExtendedStubMapping extendedStubMapping) {
        executeRequest(
                scopedAdminRoutes.requestSpecForTask(CreateExtendedStubMappingTask.class),
                PathParams.empty(),
                extendedStubMapping,
                Void.class,
                201
        );
    }

    @Override
    public int count(ExtendedRequestPattern requestPattern) {
        Map objectNode = executeRequest(
                scopedAdminRoutes.requestSpecForTask(CountByExtendedRequestPatternTask.class),
                PathParams.empty(),
                requestPattern,
                Map.class,
                200
        );
        return (Integer)objectNode.get("count");
    }

    @Override
    public void registerResourceRoot(String name, ResourceContainer root) {
        this.resourceRoots.put(name, root);
    }

    @Override
    public void saveRecordingsForRequestPattern(ExtendedRequestPattern pattern, ResourceContainer recordingDirectory) {
        executeRequest(
                scopedAdminRoutes.requestSpecForTask(JournalTask.class),
                PathParams.empty(),
                new ExchangeJournalRequest(JournalMode.RECORD,recordingDirectory.getRoot().getRootName(),recordingDirectory.getPath(),pattern),
                Void.class,
                204
        );
    }

    @Override
    public void serveRecordedMappingsAt(ResourceContainer directoryRecordedTo, ExtendedRequestPattern requestPattern, int priority) {
        executeRequest(
                scopedAdminRoutes.requestSpecForTask(JournalTask.class),
                PathParams.empty(),
                new ExchangeJournalRequest(JournalMode.PLAYBACK,directoryRecordedTo.getRoot().getRootName(),directoryRecordedTo.getPath(),requestPattern,priority),
                Void.class,
                204
        );
    }

    @Override
    public GlobalCorrelationState startNewGlobalScope(GlobalCorrelationState state) {
        return executeRequest(
                scopedAdminRoutes.requestSpecForTask(StartNewGlobalScopeTask.class),
                PathParams.empty(),
                state,
                GlobalCorrelationState.class,
                200
        );
    }

    @Override
    public ResourceContainer getResourceRoot(String resourceRoot) {
        return resourceRoots.get(resourceRoot);
    }

    @Override
    public GlobalCorrelationState stopGlobalScope(GlobalCorrelationState state) {

        return executeRequest(
                scopedAdminRoutes.requestSpecForTask(StopGlobalScopeTask.class),
                PathParams.empty(),
                state,
                GlobalCorrelationState.class,
                200
        );
    }

    public int port() {
        return port;
    }

    @Override
    public String host() {
        return this.host;
    }

    @Override
    public String baseUrl() {
        return "http://" + host() + ":" + port();
    }

    @Override
    public CorrelationState startNestedScope(InitialScopeState knownScope) {
        return executeRequest(
                scopedAdminRoutes.requestSpecForTask(StartNestedScope.class),
                PathParams.empty(),
                knownScope,
                CorrelationState.class,
                200
        );
    }

    @Override
    public CorrelationState getCorrelatedScope(String scopePath) {
        return executeRequest(
                scopedAdminRoutes.requestSpecForTask(GetCorrelatedScopeTask.class),
                PathParams.empty(),
                new CorrelationState(scopePath),
                CorrelationState.class,
                200
        );
    }

    @Override
    public List<String> stopNestedScope(CorrelationState state) {
        List list = executeRequest(
                scopedAdminRoutes.requestSpecForTask(StopNestedScopeTask.class),
                PathParams.empty(),
                state,
                List.class,
                200
        );
        return list;
    }
    @Override
    public CorrelationState startUserScope(InitialScopeState knownScope) {
        return executeRequest(
                scopedAdminRoutes.requestSpecForTask(StartUserScopeTask.class),
                PathParams.empty(),
                knownScope,
                CorrelationState.class,
                200
        );
    }


    @Override
    public List<RecordedExchange> findMatchingExchanges(ExtendedRequestPattern requestPattern) {
        CollectionLikeType type = Json.getObjectMapper().getTypeFactory().constructCollectionType(List.class, RecordedExchange.class);
        return executeRequest(
                scopedAdminRoutes.requestSpecForTask(FindExchangesInScopeTask.class),
                PathParams.empty(),
                requestPattern,
                type,
                200
        );
    }

    @Override
    public void syncCorrelatedScope(CorrelationState correlationState) {
        executeRequest(
                scopedAdminRoutes.requestSpecForTask(SyncCorrelatedScopeTask.class),
                PathParams.empty(),
                correlationState,
                Void.class,
                204
        );
    }

    @Override
    public List<StubMapping> getMappingsInScope(String scopePath) {
        CollectionLikeType type = Json.getObjectMapper().getTypeFactory().constructCollectionType(List.class, StubMapping.class);
        return executeRequest(
                scopedAdminRoutes.requestSpecForTask(GetMappingsInScopeTask.class),
                PathParams.empty(),
                new CorrelationState(scopePath),
                type,
                200
        );
    }

    @Override
    public void startStep(CorrelationState state) {
        executeRequest(
                scopedAdminRoutes.requestSpecForTask(StartStepTask.class),
                PathParams.empty(),
                state,
                Void.class,
                200
        );
    }

    @Override
    public void stopStep(CorrelationState state) {
        executeRequest(
                scopedAdminRoutes.requestSpecForTask(StopStepTask.class),
                PathParams.empty(),
                state,
                Void.class,
                200
        );
    }


    @Override
    public List<RecordedExchange> findExchangesAgainstStep(String scopePath, String stepName) {
        CollectionLikeType type = Json.getObjectMapper().getTypeFactory().constructCollectionType(List.class, RecordedExchange.class);
        return executeRequest(
                scopedAdminRoutes.requestSpecForTask(FindExchangesAgainstStepTask.class),
                PathParams.empty(),
                new CorrelationState(scopePath, stepName),
                type,
                200
        );
    }

    @Override
    public void resetAll() {
        executeRequest(
                scopedAdminRoutes.requestSpecForTask(ResetAllScopesTask.class),Void.class
        );
    }
}
