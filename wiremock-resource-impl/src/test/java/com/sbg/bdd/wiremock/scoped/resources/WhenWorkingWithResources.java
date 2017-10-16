package com.sbg.bdd.wiremock.scoped.resources;


import com.sbg.bdd.wiremock.scoped.*;
import com.sbg.bdd.wiremock.scoped.resources.*;
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class WhenWorkingWithResources extends ScopedWireMockTest {

    @Test
    public void shouldListResourcesFromTheRoot() throws IOException {
        //Given
        WireMockResourceRoot root = new WireMockResourceRoot(getWireMock().baseUrl(),"root");
        //When
        WireMockResource[] list = root.list();
        //Then
        assertThat(list.length,is(2));
        assertThat(list[0].getName(),is("scoped-wiremock-common-marker.txt"));
        assertThat(list[1].getName(),is("some_recording_dir"));
    }
    @Test
    public void shouldListResourcesFromAGivenDirectory() throws IOException {
        //Given
        WireMockResourceRoot root = new WireMockResourceRoot(getWireMock().baseUrl(),"root");
        WireMockResourceContainer someRecordingDir = (WireMockResourceContainer) root.getChild("some_recording_dir");
        //When
        WireMockResource[] list = someRecordingDir.list();
        //Then
        assertThat(list.length,is(4));
        assertThat(list[0].getName(),is("service_GET_operation1_0.headers.json"));
        assertThat(list[1].getName(),is("service_GET_operation1_0.json"));
        assertThat(list[2].getName(),is("service_PUT_operation2_0.headers.json"));
        assertThat(list[3].getName(),is("service_PUT_operation2_0.xml"));
    }
    @Test
    public void shouldReadAFile() throws IOException {
        //Given
        WireMockResourceRoot root = new WireMockResourceRoot(getWireMock().baseUrl(),"root");
        WireMockResourceContainer someRecordingDir = (WireMockResourceContainer) root.getChild("some_recording_dir");
        ReadableWireMockResource file= (ReadableWireMockResource) someRecordingDir.resolveExisting("service_GET_operation1_0.json");
        //When
        String content = new String(file.read());
        //Then
        assertThat(content,is("{}"));
    }
    @Test
    public void shouldWriteAFile() throws IOException {
        //Given
        File rootFile = ScopedWireMockTest.getDirectoryResourceRoot().getFile();
        WireMockResourceRoot root = new WireMockResourceRoot(getWireMock().baseUrl(),"root");
        WireMockResourceContainer someRecordingDir = (WireMockResourceContainer) root.getChild("some_recording_dir");
        WritableWireMockResource file= someRecordingDir.resolvePotential("sample_output.txt");
        //When
        file.write("hello world".getBytes());
        //Then
        String found=FileUtils.readFileToString(new File(rootFile, "some_recording_dir/sample_output.txt"),"UTF-8");
        assertThat(found,is("hello world"));
    }
    @Before
    public void setup() {
        File rootFile = ScopedWireMockTest.getDirectoryResourceRoot().getFile();
        new File(rootFile, "some_recording_dir/sample_output.txt").delete();
    }
    @After
    public void cleanup() {
        File rootFile = ScopedWireMockTest.getDirectoryResourceRoot().getFile();
        new File(rootFile, "some_recording_dir/sample_output.txt").delete();
    }

    protected TestRule createWireMockRule() {
        ScopedWireMockServerRule server = new ScopedWireMockServerRule();
        server.registerResourceRoot("root", ScopedWireMockTest.getDirectoryResourceRoot());
        return server;
    }
}
