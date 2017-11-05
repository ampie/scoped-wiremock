package com.sbg.bdd.wiremock.scoped.server.recording;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.Resource;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.resource.ResourceFilter;
import com.sbg.bdd.resource.file.ReadableFileResource;
import com.sbg.bdd.wiremock.scoped.admin.model.*;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.server.AbstractCorrelatedScope;
import com.sbg.bdd.wiremock.scoped.server.CorrelatedScope;
import com.sbg.bdd.wiremock.scoped.server.CorrelatedScopeAdmin;
import com.sbg.bdd.wiremock.scoped.server.UserScope;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sbg.bdd.wiremock.scoped.common.MimeTypeHelper.calculateExtension;

public class ExchangeRecorder {
    private final Handlebars handlebars;
    CorrelatedScopeAdmin scopedAdmin;
    Admin admin;

    public ExchangeRecorder(CorrelatedScopeAdmin scopedAdmin, Admin admin) {
        this.scopedAdmin = scopedAdmin;
        this.admin = admin;
        this.handlebars = new Handlebars();
    }

    public void saveRecordingsForRequestPattern(ExtendedRequestPattern pattern, ResourceContainer recordingDirectory) {
        List<RecordedExchange> recordedExchanges = scopedAdmin.findMatchingExchanges(pattern);
        for (int i = 0; i < recordedExchanges.size(); i++) {
            writeFiles(recordingDirectory, recordedExchanges.get(i));
        }
    }

    public List<MappingBuilder> serveRecordedMappingsAt(ResourceContainer directoryRecordedTo, ExtendedRequestPattern requestPattern, int priority) {
        List<MappingBuilder> mappingBuilders = new ArrayList<>();
        List<String> baseNames = extractMappingFileBaseNames(directoryRecordedTo);
        Map<String, ReadableFileResource> mappingFiles = mappingFilesByBaseName(directoryRecordedTo, baseNames);
        for (Map.Entry<String, ReadableFileResource> mappingFile : mappingFiles.entrySet()) {
            MappingBuilder currentMapping = buildMappingIfPossible(directoryRecordedTo, requestPattern, mappingFile);
            if (currentMapping != null) {
                currentMapping.atPriority(priority);
                admin.addStubMapping(currentMapping.build());
                mappingBuilders.add(currentMapping);
            }
        }
        return mappingBuilders;
    }

    private Map<String, ReadableFileResource> mappingFilesByBaseName(ResourceContainer directoryRecordedTo, List<String> baseNames) {
        Map<String, ReadableFileResource> mappingFiles = new HashMap<>();
        for (Resource file : directoryRecordedTo.list()) {
            for (String baseName : baseNames) {
                String fileName = file.getName();
                if (fileName.substring(0, fileName.lastIndexOf('.')).equals(baseName)) {
                    mappingFiles.put(baseName, (ReadableFileResource) file);
                    break;
                }
            }
        }
        return mappingFiles;
    }

    private List<String> extractMappingFileBaseNames(ResourceContainer directoryRecordedTo) {
        List<String> baseNames = new ArrayList<String>();
        Resource[] files = directoryRecordedTo.list(new ResourceFilter() {
            @Override
            public boolean accept(ResourceContainer container, String name) {
                return name.endsWith(".headers.json");
            }
        });
        for (Resource file : files) {
            baseNames.add(file.getName().substring(0, file.getName().length() - ".headers.json".length()));
        }
        return baseNames;
    }


    private void writeFiles(ResourceContainer dir, RecordedExchange recordedExchange) {
        RecordedRequest recordedRequest = recordedExchange.getRequest();
        RecordedResponse recordedResponse = recordedExchange.getResponse();
        String baseFileName = buildBaseFileName(recordedRequest);
        writeMessage(dir, baseFileName, recordedResponse);
        writeResponseHeaders(dir, baseFileName, recordedExchange);
        writeMessage(dir, baseFileName + ".request_body", recordedExchange.getRequest());
        writeRequestHeaders(dir, baseFileName, recordedExchange.getRequest());
    }

    private String buildBaseFileName(RecordedRequest recordedRequest) {
        String requestedUrl = recordedRequest.getRequestedUrl();
        if (requestedUrl.endsWith("/")) {
            requestedUrl = requestedUrl.substring(0, requestedUrl.length() - 1);
        }
        if (requestedUrl.startsWith("/")) {
            requestedUrl = requestedUrl.substring(1);
        }
        String[] split = requestedUrl.split("/");
        String urlPart = "root";
        if (split.length == 1) {
            urlPart = split[0];
        } else if (split.length > 1) {
            urlPart = split[0] + "_" + split[1];
        }
        String baseFileName = recordedRequest.getMethod().value() + "_" + urlPart + "_" + recordedRequest.getThreadContextId() + "_" + recordedRequest.getSequenceNumber();
        return baseFileName;
    }

    private void writeResponseHeaders(ResourceContainer dir, String baseFileName, RecordedExchange exchange) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers=filterHeaders(headers);
        headers = headers.
                plus(new HttpHeader("duration", String.valueOf(exchange.getDuration()))).
                plus(new HttpHeader("requestedUrl", exchange.getRequest().getRequestedUrl())).
                plus(new HttpHeader("responseCode", exchange.getResponse().getStatus() + ""));
        dir.resolvePotential(baseFileName + ".headers.json").write(Json.write(headers).getBytes());
    }

    private void writeRequestHeaders(ResourceContainer dir, String baseFileName, RecordedRequest recordedRequest) {
        HttpHeaders headers = recordedRequest.getHeaders();
        headers=filterHeaders(headers);
        dir.resolvePotential(baseFileName + ".request_headers.json").write(Json.write(headers).getBytes());
    }

    private HttpHeaders filterHeaders(HttpHeaders headers) {
        HttpHeaders result = new HttpHeaders();
        for (HttpHeader httpHeader : headers.all()) {
            if(shouldRetain(httpHeader))
            result=result.plus(httpHeader);
        }
        return result;
    }

    private boolean shouldRetain(HttpHeader httpHeader) {
        return !(httpHeader.key().equals(HeaderName.ofTheCorrelationKey()) || httpHeader.key().equals(HeaderName.ofTheServiceInvocationCount()));
    }

    private void writeMessage(ResourceContainer dir, String baseFileName, RecordedMessage recordedResponse) {
        String extension = calculateExtension(recordedResponse.getHeaders());
        String base64Body = recordedResponse.getBase64Body();
        byte[] body = Base64.decodeBase64(base64Body.getBytes());
        dir.resolvePotential(baseFileName + extension).write(body);
    }

    private String buildBaseFileName(String requestedUrl, String sequenceNumber, String httpMethod) {
        if (requestedUrl.endsWith("/")) {
            requestedUrl = requestedUrl.substring(0, requestedUrl.length() - 1);
        }
        if (requestedUrl.startsWith("/")) {
            requestedUrl = requestedUrl.substring(1);
        }
        String[] split = requestedUrl.split("/");
        String urlPart = "root";
        if (split.length == 1) {
            urlPart = split[0];
        } else if (split.length > 1) {
            urlPart = split[0] + "_" + split[1];
        }
        return httpMethod + "_" + urlPart + "_" + sequenceNumber;
    }


    private MappingBuilder buildMappingIfPossible(ResourceContainer directoryRecordedTo, ExtendedRequestPattern templateRequestPattern, Map.Entry<String, ReadableFileResource> entry) {
        String body = buildBody(templateRequestPattern, entry);
        ReadableResource headersResource = (ReadableResource) directoryRecordedTo.resolveExisting(entry.getKey() + ".headers.json");
        HttpHeaders headers = Json.read(new String(headersResource.read()), HttpHeaders.class);
        Pattern compile = Pattern.compile("(GET|PUT|POST|DELETE|HEADE|PATCH)_(.*)_(\\d+)_(\\d+)");
        Matcher s = compile.matcher(entry.getKey());
        if (s.find()) {
            UrlPathPattern urlPattern = calculateRequestUrl(templateRequestPattern, headers);
            MappingBuilder mappingBuilder = WireMock.request(s.group(1), urlPattern);
            mappingBuilder.withHeader(HeaderName.ofTheThreadContextId(), WireMock.equalTo(s.group(3)));
            mappingBuilder.withHeader(HeaderName.ofTheSequenceNumber(), WireMock.equalTo(s.group(4)));
            copyTemplateInto(templateRequestPattern, mappingBuilder);
            return mappingBuilder.willReturn(WireMock.aResponse().withHeaders(headers).withBody(body).withStatus(calculateResponseCode(headers)));
        } else {
            return null;
        }
    }

    private String buildBody(ExtendedRequestPattern templateRequestPattern, Map.Entry<String, ReadableFileResource> entry) {
        try {
            Template template = handlebars.compileInline(new String(entry.getValue().read()));
            AbstractCorrelatedScope scope = scopedAdmin.getAbstractCorrelatedScope(templateRequestPattern.getCorrelationPath());
            return template.apply(aggregateTemplateVariables(scope));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> aggregateTemplateVariables(AbstractCorrelatedScope scope) {
        Map<String, Object> templateVariables = new HashMap<>();
        if (scope instanceof CorrelatedScope) {
            addTemplateVariablesFromAncestors((CorrelatedScope) scope, null, templateVariables);
        } else if (scope instanceof UserScope) {
            addTemplateVariablesFromAncestors(scope.getParent(), scope.getName(), templateVariables);
        }
        return templateVariables;
    }

    private void addTemplateVariablesFromAncestors(CorrelatedScope scope,String userScopeId, Map<String, Object> templateVariables) {
        if (scope.getParent() != null) {
            addTemplateVariablesFromAncestors(scope.getParent(), userScopeId, templateVariables);
        }
        templateVariables.putAll(scope.getTemplateVariables());
        if(userScopeId!=null && scope.getUserScope(userScopeId)!=null){
            //userscope overrides everybodyscope
            templateVariables.putAll(scope.getUserScope(userScopeId).getTemplateVariables());
        }

    }

    private void copyTemplateInto(ExtendedRequestPattern templateRequestPattern, MappingBuilder mappingBuilder) {
        if (templateRequestPattern.getBodyPatterns() != null) {
            for (StringValuePattern pattern : templateRequestPattern.getBodyPatterns()) {
                mappingBuilder.withRequestBody(pattern);
            }
        }
        for (Map.Entry<String, MultiValuePattern> h : templateRequestPattern.getHeaders().entrySet()) {
            mappingBuilder.withHeader(h.getKey(), h.getValue().getValuePattern());
        }
        if (templateRequestPattern.getQueryParameters() != null) {
            for (Map.Entry<String, MultiValuePattern> q : templateRequestPattern.getQueryParameters().entrySet()) {
                mappingBuilder.withQueryParam(q.getKey(), q.getValue().getValuePattern());
            }
        }
        BasicCredentials auth = templateRequestPattern.getBasicAuthCredentials();
        if (auth != null) {
            mappingBuilder.withBasicAuth(auth.username, auth.password);
        }
    }

    private UrlPathPattern calculateRequestUrl(ExtendedRequestPattern requestPattern, HttpHeaders headers) {
        if (headers.getHeader("requestedUrl").isPresent()) {
            return WireMock.urlPathEqualTo(headers.getHeader("requestedUrl").firstValue());
        } else {
            return (UrlPathPattern) requestPattern.getUrlMatcher();
        }
    }

    private int calculateResponseCode(HttpHeaders headers) {
        int responseCode = 200;
        if (headers.getHeader("responseCode").isPresent()) {
            responseCode = Integer.parseInt(headers.getHeader("responseCode").firstValue());
        }
        return responseCode;
    }


}
