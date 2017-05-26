package com.sbg.bdd.wiremock.scoped.client;


import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedRequest;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedResponse;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.extended.ExtendedMappingBuilder;
import com.sbg.bdd.wiremock.scoped.extended.ExtendedRequestPatternBuilder;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sbg.bdd.wiremock.scoped.strategies.MimeTypeHelper.calculateExtension;


public class RecordingWireMockClient extends ScopedWireMock {

    public RecordingWireMockClient(ScopedAdmin admin) {
        super(admin);
    }

    public RecordingWireMockClient(URL wireMockBaseUrl) {
        super(wireMockBaseUrl.getHost(), wireMockBaseUrl.getPort(), wireMockBaseUrl.getPath());
    }

    public void saveRecordingsForRequestPattern(StringValuePattern scopePath, RequestPattern pattern, File recordingDirectory) {
        try {
            if (!(recordingDirectory.exists() || recordingDirectory.mkdirs())) {
                throw new IllegalStateException("Could not create dir: " + recordingDirectory.getAbsolutePath());
            }
            List<RecordedExchange> recordedExchanges = findMatchingExchanges(scopePath, pattern);
            for (int i = 0; i < recordedExchanges.size(); i++) {
                writeFiles(recordingDirectory, recordedExchanges.get(i));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<MappingBuilder> serveRecordedMappingsAt(File directoryRecordedTo, ExtendedRequestPatternBuilder requestPattern, int priority) {
        try {
            List<MappingBuilder> mappingBuilders = new ArrayList<>();
            if (directoryRecordedTo.exists()) {
                List<String> baseNames = extractMappingFileBaseNames(directoryRecordedTo);
                Map<String, File> mappingFiles = mappingFilesByBaseName(directoryRecordedTo, baseNames);
                for (Map.Entry<String, File> mappingFile : mappingFiles.entrySet()) {
                    ExtendedMappingBuilder currentMapping = buildMappingIfPossible(directoryRecordedTo, requestPattern, mappingFile);
                    if (currentMapping != null) {
                        currentMapping.atPriority(priority);
                        register(currentMapping);
                        mappingBuilders.add(currentMapping);
                    }
                }
            }
            return mappingBuilders;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, File> mappingFilesByBaseName(File directoryRecordedTo, List<String> baseNames) {
        Map<String, File> mappingFiles = new HashMap<>();
        for (File file : directoryRecordedTo.listFiles()) {
            for (String baseName : baseNames) {
                if (file.getName().startsWith(baseName) && !file.getName().endsWith(".headers.json")) {
                    mappingFiles.put(baseName, file);
                    break;
                }
            }
        }
        return mappingFiles;
    }

    private List<String> extractMappingFileBaseNames(File directoryRecordedTo) {
        List<String> baseNames = new ArrayList<String>();
        if (directoryRecordedTo.exists()) {
            File[] files = directoryRecordedTo.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".headers.json");
                }
            });
            for (File file : files) {
                baseNames.add(file.getName().substring(0, file.getName().length() - ".headers.json".length()));
            }
        }
        return baseNames;
    }


    private void writeFiles(File dir, RecordedExchange recordedExchange) throws Exception {
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
        FileUtils.writeByteArrayToFile(new File(dir, baseFileName + extension), body);
        headers = headers.
                plus(new HttpHeader("requestedUrl", requestedUrl)).
                plus(new HttpHeader("responseCode", recordedResponse.getStatus() + ""));
        FileUtils.write(new File(dir, baseFileName + ".headers.json"), Json.write(headers));
    }


    private String buildBaseFileName(String requestedUrl, String sequenceNumber, String httpMethod) {
        String[] segments = requestedUrl.split("/");
        String serviceName = segments[segments.length - 2];
        String operation = segments[segments.length - 1];
        return String.join("_", serviceName, httpMethod, operation, sequenceNumber);
    }


    private ExtendedMappingBuilder buildMappingIfPossible(File directoryRecordedTo, ExtendedRequestPatternBuilder requestPatternBuilder, Map.Entry<String, File> entry) throws IOException {
        String body = FileUtils.readFileToString(new File(directoryRecordedTo, entry.getValue().getName()));
        HttpHeaders headers = Json.read(FileUtils.readFileToString(new File(directoryRecordedTo, entry.getKey() + ".headers.json")), HttpHeaders.class);
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
