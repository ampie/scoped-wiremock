package com.sbg.bdd.wiremock.scoped.recording;


import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.Resource;
import com.sbg.bdd.resource.ResourceFilter;
import com.sbg.bdd.resource.file.ReadableFileResource;
import com.sbg.bdd.wiremock.scoped.recording.builders.ExtendedMappingBuilder;
import com.sbg.bdd.wiremock.scoped.recording.builders.ExtendedRequestPatternBuilder;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedRequest;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedResponse;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.ScopedWireMockClient;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sbg.bdd.resource.ResourceContainer;

import static com.sbg.bdd.wiremock.scoped.recording.strategies.MimeTypeHelper.calculateExtension;


public class RecordingWireMockClient extends ScopedWireMockClient {

    public RecordingWireMockClient(ScopedAdmin admin) {
        super(admin);
    }

    public RecordingWireMockClient(URL wireMockBaseUrl) {
        super(wireMockBaseUrl.getHost(), wireMockBaseUrl.getPort(), wireMockBaseUrl.getPath());
    }

    public int count(ExtendedRequestPatternBuilder requestPatternBuilder) {
        Admin admin = (Admin) this.admin;
        return admin.countRequestsMatching(requestPatternBuilder.build()).getCount();
    }

    public void saveRecordingsForRequestPattern(StringValuePattern scopePath, RequestPattern pattern, ResourceContainer recordingDirectory) {
        List<RecordedExchange> recordedExchanges = findMatchingExchanges(scopePath, pattern);
        for (int i = 0; i < recordedExchanges.size(); i++) {
            writeFiles(recordingDirectory, recordedExchanges.get(i));
        }
    }

    public List<MappingBuilder> serveRecordedMappingsAt(ResourceContainer directoryRecordedTo, ExtendedRequestPatternBuilder requestPattern, int priority) {
        List<MappingBuilder> mappingBuilders = new ArrayList<>();
        List<String> baseNames = extractMappingFileBaseNames(directoryRecordedTo);
        Map<String, ReadableFileResource> mappingFiles = mappingFilesByBaseName(directoryRecordedTo, baseNames);
        for (Map.Entry<String, ReadableFileResource> mappingFile : mappingFiles.entrySet()) {
            ExtendedMappingBuilder currentMapping = buildMappingIfPossible(directoryRecordedTo, requestPattern, mappingFile);
            if (currentMapping != null) {
                currentMapping.atPriority(priority);
                register(currentMapping);
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
        byte[] body = Base64.getDecoder().decode(base64Body);
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
        return String.join("_", serviceName, httpMethod, operation, sequenceNumber);
    }


    private ExtendedMappingBuilder buildMappingIfPossible(ResourceContainer directoryRecordedTo, ExtendedRequestPatternBuilder requestPatternBuilder, Map.Entry<String, ReadableFileResource> entry) {
        String body = new String(entry.getValue().read());
        ReadableResource headersResource = (ReadableResource) directoryRecordedTo.resolveExisting(entry.getKey() + ".headers.json");
        HttpHeaders headers = Json.read(new String(headersResource.read()), HttpHeaders.class);
        Pattern compile = Pattern.compile("(.*)_(GET|PUT|POST|DELETE|HEADE|PATCH)_(.*)_(\\d+)");
        Matcher s = compile.matcher(entry.getKey());
        if (s.find()) {
            String urlPattern = calculateRequestUrl(requestPatternBuilder, headers, s);
            //TODO copy the rest of the RequestPattern state, e.g. headers, etc.
            ExtendedRequestPatternBuilder cloneRequestPatternbuilder = new ExtendedRequestPatternBuilder(requestPatternBuilder, RequestMethod.fromString(s.group(2)));
            ExtendedMappingBuilder mappingBuilder = new ExtendedMappingBuilder(cloneRequestPatternbuilder, null, null);
            mappingBuilder.to(urlPattern).withHeader(HeaderName.ofTheSequenceNumber(), equalTo(s.group(4)));

            return mappingBuilder.willReturn(aResponse().withHeaders(headers).withBody(body).withStatus(calculateResponseCode(headers)));
        } else {
            return null;
        }
    }

    private String calculateRequestUrl(ExtendedRequestPatternBuilder requestPattern, HttpHeaders headers, Matcher s) {
        if (headers.getHeader("requestedUrl").isPresent()) {
            return headers.getHeader("requestedUrl").firstValue();
        }
        String urlRegex;
        String prefix = requestPattern.getUrlPathPattern().getExpected();
        if (prefix.endsWith(".*")) {
            prefix = prefix.substring(0, prefix.length() - 2);
        }
        String suffix = "/" + s.group(1) + "/" + s.group(3);
        if (suffix.startsWith(prefix)) {
            urlRegex = prefix + suffix.substring(prefix.length());
        } else if (prefix.endsWith(suffix)) {
            urlRegex = prefix;
        } else {
            //TODO what if the middle segment matches? low priority, let's just use the freakin requestedUrl header
            urlRegex = prefix + ".*" + s.group(1) + "/" + s.group(3);
        }
        return urlRegex;
    }

    private int calculateResponseCode(HttpHeaders headers) {
        int responseCode = 200;
        if (headers.getHeader("responseCode").isPresent()) {
            responseCode = Integer.parseInt(headers.getHeader("responseCode").firstValue());
        }
        return responseCode;
    }


}
