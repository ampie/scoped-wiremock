package com.sbg.bdd.wiremock.scoped.client;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.sbg.bdd.wiremock.scoped.admin.*;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.common.HasBaseUrl;
import com.github.tomakehurst.wiremock.admin.AdminRoutes;
import com.github.tomakehurst.wiremock.admin.AdminTask;
import com.github.tomakehurst.wiremock.admin.RequestSpec;
import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.admin.tasks.FindNearMissesForRequestPatternTask;
import com.github.tomakehurst.wiremock.admin.tasks.FindRequestsTask;
import com.github.tomakehurst.wiremock.admin.tasks.GetRequestCountTask;
import com.github.tomakehurst.wiremock.admin.tasks.OldRemoveStubMappingTask;
import com.github.tomakehurst.wiremock.client.HttpAdminClient;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.common.AdminException;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.extension.AdminApiExtension;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.FindNearMissesResult;
import com.github.tomakehurst.wiremock.verification.FindRequestsResult;
import com.github.tomakehurst.wiremock.verification.VerificationResult;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;
import com.sbg.bdd.wiremock.scoped.admin.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.common.HttpClientUtils.getEntityAsStringAndCloseStream;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sbg.bdd.wiremock.scoped.common.Reflection.getValue;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.apache.http.HttpHeaders.HOST;

public class ScopedHttpAdminClient extends HttpAdminClient implements ScopedAdmin, HasBaseUrl {
    private static final String ADMIN_URL_PREFIX = "%s://%s:%d%s/__admin";
    private final AdminRoutes scopedAdminRoutes;
    private final String scheme;
    private final String host;
    private final int port;
    private final String urlPathPrefix;
    private final String hostHeader;
    private static CloseableHttpClient httpClientOverride;

    public static void overrideHttpClient(CloseableHttpClient httpClient) {
        httpClientOverride = httpClient;
    }

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

    public int port() {
        return port;
    }

    @Override
    public String host() {
        return this.host;
    }

    @Override
    public String baseUrl(){
        return "http://" + host() + ":" + port();
    }

    @Override
    public CorrelationState startNewCorrelatedScope(String parentScopePath) {
        return executeRequest(
                scopedAdminRoutes.requestSpecForTask(StartNewCorrelatedScopeTask.class),
                PathParams.empty(),
                new CorrelationState(parentScopePath),
                CorrelationState.class,
                200
        );
    }

    @Override
    public CorrelationState joinKnownCorrelatedScope(CorrelationState knownScope) {
        return executeRequest(
                scopedAdminRoutes.requestSpecForTask(JoinKnownCorrelatedScopeTask.class),
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
    public List<String> stopCorrelatedScope(String scopePath) {
        List list = executeRequest(
                scopedAdminRoutes.requestSpecForTask(StopCorrelatedScopeTask.class),
                PathParams.empty(),
                new CorrelationState(scopePath),
                List.class,
                200
        );
        return list;
    }

    @Override
    public List<RecordedExchange> findMatchingExchanges(RequestPattern requestPattern) {
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
    public FindNearMissesResult findTopNearMissesFor(RequestPattern requestPattern) {
        String url = this.urlFor(FindNearMissesForRequestPatternTask.class);
        String json = Json.write(requestPattern);
        HttpPost post = new HttpPost(url);
        post.setEntity(jsonStringEntity(json));
        String body = this.safelyExecuteRequest(url, HTTP_OK, post);
        return Json.read(body, FindNearMissesResult.class);
    }

    @Override
    public FindRequestsResult findRequestsMatching(RequestPattern requestPattern) {
        String url = urlFor(FindRequestsTask.class);
        String json = Json.write(requestPattern);
        HttpPost post = new HttpPost(url);
        post.setEntity(jsonStringEntity(json));
        String body = this.safelyExecuteRequest(url, HTTP_OK, post);
        return Json.read(body, FindRequestsResult.class);
    }

    @Override
    public VerificationResult countRequestsMatching(RequestPattern requestPattern) {
        String url = this.urlFor(GetRequestCountTask.class);
        String json = Json.write(requestPattern);
        HttpPost post = new HttpPost(url);
        post.setEntity(jsonStringEntity(json));
        return VerificationResult.from(this.safelyExecuteRequest(url, HTTP_OK, post));
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
    public void startStep(String scopePath, String stepName) {
        executeRequest(
                scopedAdminRoutes.requestSpecForTask(StartStepTask.class),
                PathParams.empty(),
                new CorrelationState(scopePath, stepName),
                Void.class,
                200
        );
    }

    @Override
    public void stopStep(String scopePath, String stepName) {
        executeRequest(
                scopedAdminRoutes.requestSpecForTask(StopStepTask.class),
                PathParams.empty(),
                new CorrelationState(scopePath, stepName),
                Void.class,
                200
        );
    }

    @Override
    public void removeStubMapping(StubMapping stubbMapping) {
        executeRequest(
                scopedAdminRoutes.requestSpecForTask(OldRemoveStubMappingTask.class),
                PathParams.empty(),
                stubbMapping,
                Void.class,
                200);
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

    private <B, R> R executeRequest(RequestSpec requestSpec, PathParams pathParams, B requestBody, Class<R> responseType, int expectedStatus) {
        return (R) executeRequest(requestSpec, pathParams, requestBody, Json.getObjectMapper().getTypeFactory().constructType(responseType), expectedStatus);
    }

    private <B, R> R executeRequest(RequestSpec requestSpec, PathParams pathParams, B requestBody, JavaType responseType, int expectedStatus) {
        String url = String.format(ADMIN_URL_PREFIX + requestSpec.path(pathParams), scheme, host, port, urlPathPrefix);
        RequestBuilder requestBuilder = RequestBuilder
                .create(requestSpec.method().getName())
                .setUri(url)
                .addHeader(new BasicHeader("Content-Type", "application/json"));

        if (requestBody != null) {
            requestBuilder.setEntity(jsonStringEntity(Json.write(requestBody)));
        }

        String responseBodyString = safelyExecuteRequest(url, expectedStatus, requestBuilder.build());
        if (responseType.getRawClass() == Void.class) {
            return null;
        }
        Object result;
        try {
            ObjectMapper mapper = Json.getObjectMapper();
            result = mapper.readValue(responseBodyString, responseType);
        } catch (IOException ioe) {
            result = throwUnchecked(ioe, responseType.getRawClass());
        }
        return (R) result;
    }

    private String safelyExecuteRequest(String url, int expectedStatus, HttpUriRequest request) {
        if (hostHeader != null) {
            request.addHeader(HOST, hostHeader);
        }

        try (CloseableHttpResponse response = execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != expectedStatus) {
                throw new VerificationException(
                        "Expected status " + expectedStatus + " for " + url + " but was " + statusCode + " for reason:'" + getEntityAsStringAndCloseStream(response) + "'");
            }

            return getEntityAsStringAndCloseStream(response);
        } catch (Exception e) {
            return throwUnchecked(e, String.class);
        }
    }

    private CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
        if (httpClientOverride != null) {
            CloseableHttpResponse response = httpClientOverride.execute(request);
            if (response == null) {
                String content = "";
                if (request instanceof HttpEntityEnclosingRequestBase) {
                    content = ((HttpEntityEnclosingRequestBase) request).getEntity().toString();
                }
                throw new IllegalArgumentException("Requested " + request.getMethod() + " to " + request.getURI() + " not mapped. Content: " + content);
            }
            return response;
        } else {
            return ((CloseableHttpClient) getValue(this, "httpClient")).execute(request);
        }
    }

    @Override
    public void addStubMapping(StubMapping stubMapping) {
        if (stubMapping.getRequest().hasCustomMatcher()) {
            throw new AdminException("Custom matchers can't be used when administering a remote WireMock server. " +
                    "Use WireMockRule.stubFor() or WireMockServer.stubFor() to administer the local instance.");
        }

        this.executeRequest(
                this.scopedAdminRoutes.requestSpecForTask(CreateStubMappingTask.class),
                PathParams.empty(),
                stubMapping,
                Void.class,
                201
        );
    }

    private static StringEntity jsonStringEntity(String json) {
        return new StringEntity(json, ContentType.APPLICATION_JSON);
    }

    private String urlFor(Class<? extends AdminTask> taskClass) {
        RequestSpec requestSpec = scopedAdminRoutes.requestSpecForTask(taskClass);
        checkNotNull(requestSpec, "No admin task URL is registered for " + taskClass.getSimpleName());
        return String.format(ADMIN_URL_PREFIX + requestSpec.path(), scheme, host, port, urlPathPrefix);
    }

}
