package com.sbg.bdd.wiremock.scoped.recording.strategies;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.recording.DefaultMappingPriority;
import com.sbg.bdd.wiremock.scoped.recording.WireMockContext;
import com.sbg.bdd.wiremock.scoped.recording.builders.ExtendedMappingBuilder;
import com.sbg.bdd.wiremock.scoped.recording.builders.ExtendedResponseDefinitionBuilder;
import com.sbg.bdd.wiremock.scoped.recording.builders.ResponseStrategy;
import org.apache.commons.io.output.StringBuilderWriter;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;



public class ResponseBodyStrategies {


    public static ResponseStrategy returnTheBody(final String body, final String contentType) {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception {
                builder.atPriority(scope.calculatePriority(DefaultMappingPriority.BODY_KNOWN.priority()));
                return aResponse().withBody(body).withHeader("Content-StepEventType", contentType);
            }
        };
    }

    public static ResponseStrategy returnTheFile(final String fileName) {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception {
                ReadableResource bodyFile = scope.resolveInputResource(fileName);
                String responseBody = new String(bodyFile.read());
                String headers = readHeaders(bodyFile);
                builder.atPriority(scope.calculatePriority(DefaultMappingPriority.BODY_KNOWN.priority()));
                ExtendedResponseDefinitionBuilder responseBuilder = aResponse().withBody(responseBody).withHeader("Content-StepEventType", MimeTypeHelper.determineContentType(fileName));
                if (headers != null) {
                    addHeaders(headers, responseBuilder);
                }
                return responseBuilder;
            }
        };
    }

    public static ResponseStrategy merge(final TemplateBuilder templateBuilder) {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception {
                ReadableResource templateFile = scope.resolveInputResource(templateBuilder.getFileName());
                String templateContent = new String(templateFile.read());
                String headers = readHeaders(templateFile);

                Handlebars mf = new Handlebars();
                Template mustache = mf.compileInline(templateContent);
                StringBuilderWriter writer = new StringBuilderWriter();
                mustache.apply(templateBuilder.getVariables(), writer);
                String responseBody = writer.toString();

                builder.atPriority(scope.calculatePriority(DefaultMappingPriority.BODY_KNOWN.priority()));
                ExtendedResponseDefinitionBuilder responseBuilder = aResponse().withBody(responseBody).withHeader("Content-StepEventType", MimeTypeHelper.determineContentType(templateBuilder.getFileName()));
                if (headers != null) {
                    addHeaders(headers, responseBuilder);
                }
                return responseBuilder;
            }
        };
    }

    public static void addHeaders(String headers, ResponseDefinitionBuilder responseDefinitionBuilder) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonObject = (ObjectNode) mapper.readTree(headers);
            Iterator<Map.Entry<String, JsonNode>> fields = jsonObject.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                responseDefinitionBuilder.withHeader(entry.getKey(), entry.getValue().asText());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String readHeaders(ReadableResource templateFile) throws IOException {
        String baseName = templateFile.getName().substring(0, templateFile.getName().lastIndexOf('.'));
        ResourceContainer container = templateFile.getContainer();
        ReadableResource headersFile = (ReadableResource) templateFile.getContainer().resolveExisting(baseName + ".headers.json");
        if (headersFile!=null) {
            return new String (headersFile.read());
        } else {
            return null;
        }
    }

    public static TemplateBuilder theTemplate(String templateFileName) {
        return new TemplateBuilder(templateFileName);

    }

    public static ExtendedResponseDefinitionBuilder aResponse() {
        return new ExtendedResponseDefinitionBuilder();
    }
}
