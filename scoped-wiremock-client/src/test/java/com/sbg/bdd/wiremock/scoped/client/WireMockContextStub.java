package com.sbg.bdd.wiremock.scoped.client;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.file.DirectoryResourceRoot;
import com.sbg.bdd.resource.file.ReadableFileResource;
import com.sbg.bdd.wiremock.scoped.client.builders.ExtendedMappingBuilder;
import com.sbg.bdd.wiremock.scoped.client.builders.ExtendedRequestPatternBuilder;
import com.sbg.bdd.wiremock.scoped.server.CorrelatedScope;
import com.sbg.bdd.wiremock.scoped.server.ExtendedStubMappingTranslator;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WireMockContextStub implements WireMockContext {
    static DirectoryResourceRoot srcTestResources;

    static {
        URL resource = WireMockContextStub.class.getClassLoader().getResource("scoped-wiremock-client-recording.markerfile");
        if (resource.getProtocol().equals("file")) {
            srcTestResources = new DirectoryResourceRoot("resourceRoot", new File(resource.getFile()).getParentFile());
        }
    }

    private CorrelatedScope scope;

    public static DirectoryResourceRoot getSrcTestResources() {
        return srcTestResources;
    }

    private List<StubMapping> mappings = new ArrayList<>();
    private List<ExtendedMappingBuilder> recordingMappingBuilders = new ArrayList<>();

    public WireMockContextStub(CorrelatedScope scope) {
        this.scope = scope;
    }

    public List<StubMapping> getMappings() {
        return mappings;
    }

    @Override
    public String getCorrelationPath() {
        return "asdf";
    }

    @Override
    public ReadableResource resolveInputResource(String fileName) {
        if (!getSrcTestResources().fallsWithin(fileName)) {
            File file = new File(fileName);
            return new ReadableFileResource(new DirectoryResourceRoot("resourceRoot", file.getParentFile()), file);
        }
        return (ReadableResource) getSrcTestResources().resolveExisting(fileName);
    }

    @Override
    public String getBaseUrlOfServiceUnderTest() {
        return "http://service.com/under/test";
    }

    @Override
    public void register(ExtendedMappingBuilder mapping) {
        if (mapping.getResponseDefinitionBuilder() == null) {
            recordingMappingBuilders.add(mapping);
        } else {
            List<StubMapping> list = new ExtendedStubMappingTranslator(mapping.build(), scope.getGlobalScope().getEndPointConfigRegistry(), scope).createAllSupportingStubMappings();
            for (StubMapping child : list) {
                mappings.add(child);
            }
        }
    }


    public List<ExtendedMappingBuilder> getRecordingMappingBuilders() {
        return recordingMappingBuilders;
    }

    @Override
    public int count(ExtendedRequestPatternBuilder requestPatternBuilder) {
        return 0;
    }

}
