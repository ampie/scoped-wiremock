package com.sbg.bdd.wiremock.scoped.client.strategies;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.common.MimeTypeHelper;
import com.sbg.bdd.wiremock.scoped.admin.model.ScopeLocalPriority;
import com.sbg.bdd.wiremock.scoped.client.WireMockContext;
import com.sbg.bdd.wiremock.scoped.client.builders.ExtendedMappingBuilder;
import com.sbg.bdd.wiremock.scoped.client.builders.ExtendedResponseDefinitionBuilder;
import com.sbg.bdd.wiremock.scoped.client.builders.ResponseStrategy;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

import static java.lang.String.format;


public class ResponseBodyStrategies {
    public static ResponseStrategy returnTheBody(final String body, final String contentType) {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception {
                builder.atPriority(ScopeLocalPriority.BODY_KNOWN);
                return aResponse().withBody(body).withHeader("Content-Type", contentType);
            }

            @Override
            public String getDescription() {
                return format("return the body \"%s\"", body);
            }
        };
    }

    public static ResponseStrategy returnTheFile(final String fileName) {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception {
                ReadableResource bodyFile = scope.resolveInputResource(fileName);
                String responseBody = new String(bodyFile.read());
                String headers = readHeaders(bodyFile);
                builder.atPriority(ScopeLocalPriority.BODY_KNOWN);
                ExtendedResponseDefinitionBuilder responseBuilder = aResponse().withBody(responseBody).withHeader("Content-Type", MimeTypeHelper.determineContentType(fileName));
                if (headers != null) {
                    addHeaders(headers, responseBuilder);
                }
                return responseBuilder;
            }

            @Override
            public String getDescription() {
                return format("return the file \"%s\"", fileName);
            }

        };
    }

    public static ResponseStrategy merge(final TemplateBuilder templateBuilder) {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception {
                ReadableResource templateFile = scope.resolveInputResource(templateBuilder.getFileName());
                String templateContent = new String(templateFile.read());
                String headers = readHeaders(templateFile);
                Template tmpl = Mustache.compiler().compile(templateContent);

                StringWriter writer = new StringWriter();
                tmpl.execute(templateBuilder.getVariables(), writer);
                String responseBody = writer.toString();

                builder.atPriority(ScopeLocalPriority.BODY_KNOWN);
                ExtendedResponseDefinitionBuilder responseBuilder = aResponse().withBody(responseBody).withHeader("Content-Type", MimeTypeHelper.determineContentType(templateBuilder.getFileName()));
                if (headers != null) {
                    addHeaders(headers, responseBuilder);
                }
                return responseBuilder;
            }

            @Override
            public String getDescription() {
                return format("merge the template \"%s\"", templateBuilder.getFileName());
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
        if (headersFile != null) {
            return new String(headersFile.read());
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
