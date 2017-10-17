package com.sbg.bdd.wiremock.scoped.common;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.Resource;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.resource.ResourceFilter;
import com.sbg.bdd.resource.file.ReadableFileResource;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.*;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import org.apache.commons.codec.binary.Base64;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sbg.bdd.wiremock.scoped.common.MimeTypeHelper.calculateExtension;

public class ExchangeRecorder {
    ScopedAdmin scopedAdmin;
    Admin admin;
    public ExchangeRecorder(ScopedAdmin scopedAdmin,Admin admin) {
        this.scopedAdmin = scopedAdmin;
        this.admin = admin;
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
                if (file.getName().startsWith(baseName) && !file.getName().endsWith(".headers.json")) {
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
        String requestedUrl = recordedRequest.getRequestedUrl();
        String sequenceNumber = recordedRequest.getSequenceNumber() + "";
        String httpMethod = recordedRequest.getMethod().value();
        HttpHeaders headers = recordedResponse.getHeaders();
        String extension = calculateExtension(headers);
        String baseFileName = buildBaseFileName(requestedUrl, sequenceNumber, httpMethod);
        String base64Body = recordedResponse.getBase64Body();
        byte[] body = Base64.decodeBase64(base64Body.getBytes());
        dir.resolvePotential(baseFileName + extension).write(body);
        headers = headers.
                plus(new HttpHeader("requestedUrl", requestedUrl)).
                plus(new HttpHeader("responseCode", recordedResponse.getStatus() + ""));
        dir.resolvePotential(baseFileName + ".headers.json").write(Json.write(headers).getBytes());
    }


    private String buildBaseFileName(String requestedUrl, String sequenceNumber, String httpMethod) {
        String[] segments = requestedUrl.split("/");
        String serviceName = segments[segments.length - 2];
        String operation = segments[segments.length - 1];
        return serviceName + "_" + httpMethod + "_" + operation + "_" + sequenceNumber;
    }


    private MappingBuilder buildMappingIfPossible(ResourceContainer directoryRecordedTo, ExtendedRequestPattern templateRequestPattern, Map.Entry<String, ReadableFileResource> entry) {
        String body = new String(entry.getValue().read());
        ReadableResource headersResource = (ReadableResource) directoryRecordedTo.resolveExisting(entry.getKey() + ".headers.json");
        HttpHeaders headers = Json.read(new String(headersResource.read()), HttpHeaders.class);
        Pattern compile = Pattern.compile("(.*)_(GET|PUT|POST|DELETE|HEADE|PATCH)_(.*)_(\\d+)");
        Matcher s = compile.matcher(entry.getKey());
        if (s.find()) {
            UrlPathPattern urlPattern = calculateRequestUrl(templateRequestPattern, headers, s);
            MappingBuilder mappingBuilder = WireMock.request(s.group(2), urlPattern);
            mappingBuilder.withHeader(HeaderName.ofTheSequenceNumber(), WireMock.equalTo(s.group(4)));
            copyTemplateInto(templateRequestPattern, mappingBuilder);
            return mappingBuilder.willReturn(WireMock.aResponse().withHeaders(headers).withBody(body).withStatus(calculateResponseCode(headers)));
        } else {
            return null;
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

    private UrlPathPattern calculateRequestUrl(ExtendedRequestPattern requestPattern, HttpHeaders headers, Matcher s) {
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
