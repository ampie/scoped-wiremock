package com.sbg.bdd.wiremock.scoped.client;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.admin.AdminRoutes;
import com.github.tomakehurst.wiremock.admin.AdminTask;
import com.github.tomakehurst.wiremock.admin.RequestSpec;
import com.github.tomakehurst.wiremock.admin.model.*;
import com.github.tomakehurst.wiremock.admin.tasks.*;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.common.AdminException;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.global.GlobalSettings;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.FindNearMissesResult;
import com.github.tomakehurst.wiremock.verification.FindRequestsResult;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.github.tomakehurst.wiremock.verification.VerificationResult;
import okhttp3.*;
import okio.Buffer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;

public class OkHttpAdminClient implements Admin {
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private static final String ADMIN_URL_PREFIX = "%s://%s:%d%s/__admin";

    private final String scheme;
    private final String host;
    private final int port;
    private final String urlPathPrefix;
    private final String hostHeader;

    private final AdminRoutes adminRoutes;
    protected OkHttpClient client = new OkHttpClient();

    public OkHttpAdminClient(String scheme, String host, int port) {
        this(scheme, host, port, "");
    }

    public OkHttpAdminClient(String host, int port, String urlPathPrefix) {
        this("http", host, port, urlPathPrefix);
    }

    public OkHttpAdminClient(String scheme, String host, int port, String urlPathPrefix) {
        this(scheme, host, port, urlPathPrefix, null, null, 0);
    }

    public OkHttpAdminClient(String scheme,
                             String host,
                             int port,
                             String urlPathPrefix,
                             String hostHeader,
                             String proxyHost,
                             int proxyPort) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.urlPathPrefix = urlPathPrefix;
        this.hostHeader = hostHeader;

        adminRoutes = AdminRoutes.defaults();

    }

    public OkHttpAdminClient(String host, int port) {
        this(host, port, "");
    }

    public OkHttpClient getClient() {
        return client;
    }

    public void setClient(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public void addStubMapping(StubMapping stubMapping) {
        if (stubMapping.getRequest().hasCustomMatcher()) {
            throw new AdminException("Custom matchers can't be used when administering a remote WireMock server. " +
                    "Use WireMockRule.stubFor() or WireMockServer.stubFor() to administer the local instance.");
        }

        executeRequest(
                adminRoutes.requestSpecForTask(CreateStubMappingTask.class),
                PathParams.empty(),
                stubMapping,
                Void.class,
                201
        );
    }

    @Override
    public void editStubMapping(StubMapping stubMapping) {
        postJsonAssertOkAndReturnBody(
                urlFor(OldEditStubMappingTask.class),
                Json.write(stubMapping),
                HTTP_NO_CONTENT);
    }

    @Override
    public void removeStubMapping(StubMapping stubbMapping) {
        postJsonAssertOkAndReturnBody(
                urlFor(OldRemoveStubMappingTask.class),
                Json.write(stubbMapping),
                HTTP_OK);
    }

    @Override
    public ListStubMappingsResult listAllStubMappings() {
        return executeRequest(
                adminRoutes.requestSpecForTask(GetAllStubMappingsTask.class),
                ListStubMappingsResult.class
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public SingleStubMappingResult getStubMapping(UUID id) {
        return executeRequest(
                adminRoutes.requestSpecForTask(GetStubMappingTask.class),
                PathParams.single("id", id),
                SingleStubMappingResult.class
        );
    }

    @Override
    public void saveMappings() {
        postJsonAssertOkAndReturnBody(urlFor(SaveMappingsTask.class), null, HTTP_OK);
    }

    @Override
    public void resetAll() {
        postJsonAssertOkAndReturnBody(urlFor(ResetTask.class), null, HTTP_OK);
    }

    @Override
    public void resetRequests() {
        executeRequest(adminRoutes.requestSpecForTask(ResetRequestsTask.class));
    }

    @Override
    public void resetScenarios() {
        executeRequest(adminRoutes.requestSpecForTask(ResetScenariosTask.class));
    }

    @Override
    public void resetMappings() {
        executeRequest(adminRoutes.requestSpecForTask(ResetStubMappingsTask.class));
    }

    @Override
    public void resetToDefaultMappings() {
        postJsonAssertOkAndReturnBody(urlFor(ResetToDefaultMappingsTask.class), null, HTTP_OK);
    }

    @Override
    public GetServeEventsResult getServeEvents() {
        return executeRequest(
                adminRoutes.requestSpecForTask(GetAllRequestsTask.class),
                GetServeEventsResult.class
        );
    }

    @Override
    public SingleServedStubResult getServedStub(UUID id) {
        return executeRequest(
                adminRoutes.requestSpecForTask(GetServedStubTask.class),
                PathParams.single("id", id),
                SingleServedStubResult.class
        );
    }

    @Override
    public VerificationResult countRequestsMatching(RequestPattern requestPattern) {
        String body = postJsonAssertOkAndReturnBody(
                urlFor(GetRequestCountTask.class),
                Json.write(requestPattern),
                HTTP_OK);
        return VerificationResult.from(body);
    }

    @Override
    public FindRequestsResult findRequestsMatching(RequestPattern requestPattern) {
        String body = postJsonAssertOkAndReturnBody(
                urlFor(FindRequestsTask.class),
                Json.write(requestPattern),
                HTTP_OK);
        return Json.read(body, FindRequestsResult.class);
    }

    @Override
    public FindRequestsResult findUnmatchedRequests() {
        String body = getJsonAssertOkAndReturnBody(
                urlFor(FindUnmatchedRequestsTask.class),
                HTTP_OK);
        return Json.read(body, FindRequestsResult.class);
    }

    @Override
    public FindNearMissesResult findNearMissesForUnmatchedRequests() {
        String body = getJsonAssertOkAndReturnBody(urlFor(FindNearMissesForUnmatchedTask.class), HTTP_OK);
        return Json.read(body, FindNearMissesResult.class);
    }

    @Override
    public FindNearMissesResult findTopNearMissesFor(LoggedRequest loggedRequest) {
        String body = postJsonAssertOkAndReturnBody(
                urlFor(FindNearMissesForRequestTask.class),
                Json.write(loggedRequest),
                HTTP_OK
        );

        return Json.read(body, FindNearMissesResult.class);
    }

    @Override
    public FindNearMissesResult findTopNearMissesFor(RequestPattern requestPattern) {
        String body = postJsonAssertOkAndReturnBody(
                urlFor(FindNearMissesForRequestPatternTask.class),
                Json.write(requestPattern),
                HTTP_OK
        );

        return Json.read(body, FindNearMissesResult.class);
    }

    @Override
    public void updateGlobalSettings(GlobalSettings settings) {
        postJsonAssertOkAndReturnBody(
                urlFor(GlobalSettingsUpdateTask.class),
                Json.write(settings),
                HTTP_OK);
    }

    @Override
    public void shutdownServer() {
        postJsonAssertOkAndReturnBody(urlFor(ShutdownServerTask.class), null, HTTP_OK);
    }

    public int port() {
        return port;
    }


    protected String postJsonAssertOkAndReturnBody(String url, String json, int expectedStatus) {
        Request.Builder builder = new Request.Builder();
        builder = builder.url(url);
        if (json == null) {
            json = "{}";
        }
        builder = builder.post(RequestBody.create(JSON, json));
        return safelyExecuteRequest(url, expectedStatus, builder);
    }

    protected String getJsonAssertOkAndReturnBody(String url, int expectedStatus) {
        return safelyExecuteRequest(url, expectedStatus, new Request.Builder().url(url).get());
    }

    protected void executeRequest(RequestSpec requestSpec) {
        executeRequest(requestSpec, PathParams.empty(), null, Void.class, 200);
    }


    protected <B, R> R executeRequest(RequestSpec requestSpec, Class<R> responseType) {
        return executeRequest(requestSpec, PathParams.empty(), null, responseType, 200);
    }

    protected <B, R> R executeRequest(RequestSpec requestSpec, PathParams pathParams, Class<R> responseType) {
        return executeRequest(requestSpec, pathParams, null, responseType, 200);
    }

    protected <B, R> R executeRequest(RequestSpec requestSpec, PathParams pathParams, B requestBody, Class<R> responseType, int expectedStatus) {
        JavaType javaType = Json.getObjectMapper().getTypeFactory().constructType(responseType);
        return executeRequest(requestSpec, pathParams, requestBody, javaType, expectedStatus);
    }

    protected <B, R> R executeRequest(RequestSpec requestSpec, PathParams pathParams, B requestBody, JavaType responseType, int expectedStatus) {
        String url = String.format(ADMIN_URL_PREFIX + requestSpec.path(pathParams), scheme, host, port, urlPathPrefix);
        Request.Builder builder = new Request.Builder().url(url);

        if (requestBody != null) {
            builder = builder.method(requestSpec.method().getName(), RequestBody.create(JSON, Json.write(requestBody)));
        } else {
            builder = builder.method(requestSpec.method().getName(), null);
        }

        String responseBodyString = safelyExecuteRequest(url, expectedStatus, builder);

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


    private String safelyExecuteRequest(String url, int expectedStatus, Request.Builder builder) {
        if (hostHeader != null) {
            builder.addHeader("Host", hostHeader);
        }
        try (Response response = execute(builder.build())) {
            int statusCode = response.code();
            if (statusCode != expectedStatus) {
                if (response.body() != null) {
                    throw new VerificationException(response.body().string());
                } else {
                    throw new VerificationException(
                            "Expected status " + expectedStatus + " for " + url + " but was " + statusCode);
                }
            }

            return getEntityAsStringAndCloseStream(response);
        } catch (Exception e) {
            return throwUnchecked(e, String.class);
        }
    }

    private String urlFor(Class<? extends AdminTask> taskClass) {
        RequestSpec requestSpec = adminRoutes.requestSpecForTask(taskClass);
        checkNotNull(requestSpec, "No admin task URL is registered for " + taskClass.getSimpleName());
        return String.format(ADMIN_URL_PREFIX + requestSpec.path(), scheme, host, port, urlPathPrefix);
    }

    public static String getEntityAsStringAndCloseStream(Response httpResponse) {
        try {
            return httpResponse.body().string();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            httpResponse.close();
        }

    }

    private Response execute(Request request) throws IOException {
        return client.newCall(request).execute();
    }

}

