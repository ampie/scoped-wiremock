package com.sbg.bdd.wiremock.scoped.recording;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.file.ReadableFileResource;
import com.sbg.bdd.resource.file.DirectoryResourceRoot;
import com.sbg.bdd.wiremock.scoped.recording.builders.ExtendedMappingBuilder;
import com.sbg.bdd.wiremock.scoped.recording.builders.ExtendedRequestPatternBuilder;
import com.sbg.bdd.wiremock.scoped.recording.endpointconfig.EndpointConfig;
import com.sbg.bdd.wiremock.scoped.recording.endpointconfig.RemoteEndPointConfigRegistry;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WireMockContextStub implements WireMockContext {
    static DirectoryResourceRoot srcTestResources;

    static {
        URL resource = WireMockContextStub.class.getClassLoader().getResource("scoped-wiremock-client-recording.markerfile");
        if (resource.getProtocol().equals("file")) {
            srcTestResources = new DirectoryResourceRoot("resourceRoot", new File(resource.getFile()).getParentFile());
        }
        }

    public static DirectoryResourceRoot getSrcTestResources() {
        return srcTestResources;
    }

    RemoteEndPointConfigRegistry endPointConfigRegistry;
    private List<StubMapping> mappings = new ArrayList<>();
    private List<ExtendedMappingBuilder> recordingMappingBuilders = new ArrayList<>();
    public WireMockContextStub(RemoteEndPointConfigRegistry endPointConfigRegistry) {
        this.endPointConfigRegistry = endPointConfigRegistry;
    }

    public List<StubMapping> getMappings() {
        return mappings;
    }

    @Override
    public ReadableResource resolveInputResource(String fileName) {
        if(!getSrcTestResources().fallsWithin(fileName)){
            File file = new File(fileName);
            return new ReadableFileResource(new DirectoryResourceRoot("resourceRoot", file.getParentFile()), file);
        }
        return (ReadableResource)getSrcTestResources().resolveExisting(fileName);
    }

    @Override
    public String getBaseUrlOfServiceUnderTest() {
        return "http://service.com/under/test";
    }

    @Override
    public void register(ExtendedMappingBuilder child) {
        if (child.getResponseDefinitionBuilder() == null) {
            recordingMappingBuilders.add(child);
        } else {
            mappings.add(child.build());
        }
    }

    public List<ExtendedMappingBuilder> getRecordingMappingBuilders() {
        return recordingMappingBuilders;
    }

    @Override
    public int count(ExtendedRequestPatternBuilder requestPatternBuilder) {
        return 0;
    }

    @Override
    public Integer calculatePriority(int localLevel) {
        return localLevel;
    }

    @Override
    public EndpointConfig endpointUrlFor(String serviceEndpointPropertyName) {
        return this.endPointConfigRegistry.endpointUrlFor(serviceEndpointPropertyName);
    }

    @Override
    public Set<EndpointConfig> allKnownExternalEndpoints() {
        return endPointConfigRegistry.allKnownExternalEndpoints();
    }
}
