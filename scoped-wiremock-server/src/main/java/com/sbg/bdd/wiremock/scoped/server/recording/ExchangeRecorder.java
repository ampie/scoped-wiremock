package com.sbg.bdd.wiremock.scoped.server.recording;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.matching.*;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.Resource;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.resource.ResourceFilter;
import com.sbg.bdd.resource.file.DirectoryResourceRoot;
import com.sbg.bdd.resource.file.ReadableFileResource;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.*;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.RuntimeCorrelationState;
import com.sbg.bdd.wiremock.scoped.server.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.impl.conn.Wire;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sbg.bdd.wiremock.scoped.common.MimeTypeHelper.calculateExtension;
import static com.sbg.bdd.wiremock.scoped.common.Reflection.getValue;
import static com.sbg.bdd.wiremock.scoped.common.Reflection.setValue;

//TODO needs some TLC. Became a dustbin for recording logic
public class ExchangeRecorder {
    private final Handlebars handlebars;
    CorrelatedScopeAdmin scopedAdmin;

    public ExchangeRecorder(CorrelatedScopeAdmin scopedAdmin) {
        this.scopedAdmin = scopedAdmin;
        this.handlebars = new Handlebars();
    }

    public void processRecordingSpec(ExtendedStubMapping builder, AbstractCorrelatedScope scope) {
        if (scope instanceof CorrelatedScope) {
            for (String personaDir : allPersonaIds()) {
                processRecordingSpecs(builder, ((CorrelatedScope) scope).findOrCreateUserScope(personaDir));
            }
        } else {
            processRecordingSpecs(builder, (UserScope) scope);
        }
    }

    public void saveRecordings(CorrelatedScope scope) {
        for (RecordingMappingForUser m : scope.getActiveRecordingOrPlaybackMappings(JournalMode.RECORD)) {
            UserScope userScope = scope.getUserScope(m.getUserInScopeId());
            if (userScope != null) {
                //if after completion of the scope the userScope was not created, we should not have any recordings
                ExtendedRequestPattern requestPattern = new ExtendedRequestPattern(userScope.getCorrelationPath(), m.getRequest());
                requestPattern.getHeaders().put(HeaderName.ofTheCorrelationKey(), m.deriveCorrelationPath(scope));
                this.saveRecordingsForRequestPattern(requestPattern, calculateRecordingDirectory(scope, m));
            }
        }
    }

    public void loadRecordings(CorrelatedScope scope) {
        for (RecordingMappingForUser m : scope.getActiveRecordingOrPlaybackMappings(JournalMode.PLAYBACK)) {
            ResourceContainer recordingDirectory = calculateRecordingDirectory(scope, m);
            if (recordingDirectory != null) {
                //if there is a recordingDirectory at this scope, we should create a UserScope
                UserScope forcedCreatedUserScope = scope.findOrCreateUserScope(m.getUserScope().getName());
                ExtendedRequestPattern requestPattern = new ExtendedRequestPattern(forcedCreatedUserScope.getCorrelationPath(), m.getRequest());
                requestPattern.getHeaders().put(HeaderName.ofTheCorrelationKey(), m.deriveCorrelationPath(scope));
                this.serveRecordedMappingsAt(recordingDirectory, requestPattern, ExtendedStubMappingTranslator.calculatePriority(m.priority(), forcedCreatedUserScope));
            }
        }
    }

    public SortedSet<String> allPersonaIds() {
        TreeSet<String> result = new TreeSet<>();
        ResourceContainer resourceRoot = scopedAdmin.getResourceRoot(ScopedAdmin.PERSONA_RESOURCE_ROOT);
        if (resourceRoot != null) {
            for (Resource o : resourceRoot.list(directoriesWithPersonaFiles())) {
                result.add(o.getName());
            }
        }
        return result;
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
                StubMapping stubMapping = currentMapping.build();
                if (stubMapping.getRequest().getUrlMatcher() == null) {
                    //ag tog
                    setValue(stubMapping.getRequest(), "url", WireMock.anyUrl());
                }
                scopedAdmin.addStubMapping(stubMapping);
                mappingBuilders.add(currentMapping);
            }
        }
        return mappingBuilders;
    }


    private ResourceContainer calculateRecordingDirectory(CorrelatedScope scope, RecordingMappingForUser m) {
        if (m.getRecordingSpecification().enforceJournalModeInScope()) {
            //scoped based journalling is assumed to be an automated process where potentially huge amounts of exchanges are recorded and never checked it.
            //if we wanted to investigate what went wrong, we are more interested in the run scope than the persona
            //hence runscope1/runscope1.1/scenarioscope1.1.1/userInScopeId
            if (m.getRecordingSpecification().recordToCurrentResourceDir()) {
                //Record to journalRoot in scope
                return toFile(getResourceRoot(m), scope.getRelativePath(), m.getUserInScopeId());
            } else if (!getResourceRoot(m).fallsWithin(m.getRecordingSpecification().getRecordingDirectory())) {
                return toFile(getAbsoluteRecordingDir(m), scope.getRelativePath(), m.getUserInScopeId());
            } else {
                return toFile(getResourceRoot(m), m.getRecordingSpecification().getRecordingDirectory(), scope.getRelativePath(), m.getUserInScopeId());
            }
        } else {
            //explicit recording mapping is assumed to be a more manual process during development, fewer exchanges will
            //be recorded, possibly manually modified or converted to
            //templates, and then eventually be checked in
            //process where we are more interested in the persona associated with the exchanges
            //hence userScope_id/runscope1 / runscope1 .1 / scenarioscope1 .1 .1
            if (m.getRecordingSpecification().recordToCurrentResourceDir()) {
                return toFile(getResourceRoot(m), m.getUserInScopeId(), scope.getRelativePath());
            } else if (!getResourceRoot(m).fallsWithin(m.getRecordingSpecification().getRecordingDirectory())) {
                //unlikely to be used this way
                return toFile(getAbsoluteRecordingDir(m), m.getUserInScopeId(), scope.getRelativePath());
            } else {
                //somewhere in the checked in persona dir, relative to the current resource dir
                return toFile(getResourceRoot(m), m.getUserInScopeId(), scope.getRelativePath(), m.getRecordingSpecification().getRecordingDirectory());
            }
        }
    }

    private void processRecordingSpecs(ExtendedStubMapping builder, UserScope userInScope) {
        if (builder.getRecordingSpecification().getJournalModeOverride() == JournalMode.RECORD) {
            userInScope.addRecordingMapping(new RecordingMappingForUser(userInScope, builder));
        } else if (builder.getRecordingSpecification().getJournalModeOverride() == JournalMode.PLAYBACK) {
            RecordingMappingForUser recordingMappingForUser = new RecordingMappingForUser(userInScope, builder);
            userInScope.addRecordingMapping(recordingMappingForUser);
            this.loadRecordings(userInScope.getParent());
        } else if (builder.getRecordingSpecification().enforceJournalModeInScope()) {
            RecordingMappingForUser recordingMappingForUser = new RecordingMappingForUser(userInScope, builder);
            userInScope.addRecordingMapping(recordingMappingForUser);
            if (userInScope.getJournalModeInScope() == JournalMode.PLAYBACK) {
                this.loadRecordings(userInScope.getParent());
            }
        }
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
        RecordedResponse recordedResponse = recordedExchange.getResponse();
        String baseFileName = buildBaseFileName(recordedExchange);
        writeMessage(dir, baseFileName, recordedResponse);
        writeResponseHeaders(dir, baseFileName, recordedExchange);
        writeMessage(dir, baseFileName + ".request_body", recordedExchange.getRequest());
        writeRequestHeaders(dir, baseFileName, recordedExchange.getRequest());
    }

    private String buildBaseFileName(RecordedExchange recordedExchange) {
        RecordedRequest recordedRequest = recordedExchange.getRequest();
        String requestedUrl = recordedRequest.getPath();
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
        String baseFileName = recordedRequest.getMethod().value() + "_" + urlPart + "_" + recordedExchange.getThreadContextId() + "_" + recordedExchange.getSequenceNumber();
        return baseFileName;
    }

    private void writeResponseHeaders(ResourceContainer dir, String baseFileName, RecordedExchange exchange) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers = filterHeaders(headers);
        headers = headers.
                plus(new HttpHeader("duration", String.valueOf(exchange.getDuration()))).
                plus(new HttpHeader("requestedUrl", exchange.getRequest().getPath())).
                plus(new HttpHeader("responseCode", exchange.getResponse().getStatus() + ""));
        dir.resolvePotential(baseFileName + ".headers.json").write(Json.write(headers).getBytes());
    }

    private void writeRequestHeaders(ResourceContainer dir, String baseFileName, RecordedRequest recordedRequest) {
        HttpHeaders headers = recordedRequest.getHeaders();
        headers = filterHeaders(headers);
        dir.resolvePotential(baseFileName + ".request_headers.json").write(Json.write(headers).getBytes());
    }

    private HttpHeaders filterHeaders(HttpHeaders headers) {
        HttpHeaders result = new HttpHeaders();
        for (HttpHeader httpHeader : headers.all()) {
            if (shouldRetain(httpHeader))
                result = result.plus(httpHeader);
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

    private MappingBuilder buildMappingIfPossible(ResourceContainer directoryRecordedTo, ExtendedRequestPattern templateRequestPattern, Map.Entry<String, ReadableFileResource> entry) {
        String body = buildBody(templateRequestPattern, entry);
        ReadableResource headersResource = (ReadableResource) directoryRecordedTo.resolveExisting(entry.getKey() + ".headers.json");
        HttpHeaders headers = Json.read(new String(headersResource.read()), HttpHeaders.class);
        Pattern compile = Pattern.compile("(GET|PUT|POST|DELETE|HEADE|PATCH)_(.*)_(\\d+)_(\\d+)");
        Matcher s = compile.matcher(entry.getKey());
        if (s.find()) {
            UrlPathPattern urlPattern = calculateRequestUrl(templateRequestPattern, headers);
            MappingBuilder mappingBuilder;
            SequenceNumberMatcher sequenceNumberMatcher = new SequenceNumberMatcher();
            sequenceNumberMatcher.setAdmin(scopedAdmin);
            sequenceNumberMatcher.setUrlPattern(urlPattern);
            sequenceNumberMatcher.setThreadContextId(Integer.valueOf(s.group(3)));
            sequenceNumberMatcher.setSequenceNumber(Integer.valueOf(s.group(4)));
            sequenceNumberMatcher.setCorrelationPattern(templateRequestPattern.getHeaders().get(HeaderName.ofTheCorrelationKey()).getValuePattern());
            mappingBuilder = WireMock.requestMatching(sequenceNumberMatcher);
            copyTemplateRequestPatternInto(templateRequestPattern, mappingBuilder);
            return mappingBuilder.willReturn(WireMock.aResponse().withHeaders(headers).withBody(body).withStatus(calculateResponseCode(headers)));
        } else {
            return null;
        }
    }

    private String buildBody(ExtendedRequestPattern templateRequestPattern, Map.Entry<String, ReadableFileResource> entry) {
        try {
            Template template = handlebars.compileInline(new String(entry.getValue().read()));
            AbstractCorrelatedScope scope = scopedAdmin.getAbstractCorrelatedScope(templateRequestPattern.getCorrelationPath());
            return template.apply(scope.aggregateTemplateVariables());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void copyTemplateRequestPatternInto(ExtendedRequestPattern templateRequestPattern, MappingBuilder mappingBuilder) {
        if (templateRequestPattern.getBodyPatterns() != null) {
            for (ContentPattern<?> pattern : templateRequestPattern.getBodyPatterns()) {
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


    private DirectoryResourceRoot getAbsoluteRecordingDir(RecordingMappingForUser m) {
        return new DirectoryResourceRoot("absoluteDir", new File(m.getRecordingSpecification().getRecordingDirectory()));
    }


    private ResourceContainer getResourceRoot(RecordingMappingForUser m) {
        if (m.getRecordingSpecification().enforceJournalModeInScope()) {
            return scopedAdmin.getResourceRoot(ScopedAdmin.JOURNAL_RESOURCE_ROOT);
        } else if (m.getRecordingSpecification().getJournalModeOverride() == JournalMode.RECORD) {
            return scopedAdmin.getResourceRoot(ScopedAdmin.OUTPUT_RESOURCE_ROOT);
        } else {
            return scopedAdmin.getResourceRoot(ScopedAdmin.PERSONA_RESOURCE_ROOT);
        }
    }


    private ResourceContainer toFile(ResourceContainer root, String... trailingSegments) {
        return root.resolvePotentialContainer(trailingSegments);
    }

    private ResourceFilter directoriesWithPersonaFiles() {
        return new ResourceFilter() {
            //TODO make this more generic
            @Override
            public boolean accept(ResourceContainer dir, String name) {
                Resource file = dir.resolveExisting(name);
                if (file.getName().equals("everybody")) {
                    return false;
                } else if (file instanceof ResourceContainer) {
                    return file.getName().equals(ScopedAdmin.GUEST) || hasPersonaFile((ResourceContainer) file);
                } else {
                    return false;
                }
            }

            private boolean hasPersonaFile(ResourceContainer file) {
                for (Resource resource : file.list()) {
                    if (resource instanceof ReadableResource && resource.getName().startsWith("persona.")) {
                        return true;
                    }
                }
                return false;
            }

        };
    }

}
