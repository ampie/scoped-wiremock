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
        assertThat(list.length,is(6));
        assertThat(list[0].getName(),is("example_journal"));
        assertThat(list[1].getName(),is("example_template_journal"));
        assertThat(list[2].getName(),is("personas"));
        assertThat(list[3].getName(),is("scoped-wiremock-common-marker.txt"));
        assertThat(list[4].getName(),is("some_recording_dir"));
        assertThat(list[5].getName(),is("some_resource_dir"));
    }
    @Test
    public void shouldListResourcesFromAGivenDirectory() throws IOException {
        //Given
        WireMockResourceRoot root = new WireMockResourceRoot(getWireMock().baseUrl(),"root");
        WireMockResourceContainer someRecordingDir = (WireMockResourceContainer) root.getChild("some_resource_dir");
        //When
        WireMockResource[] list = someRecordingDir.list();
        //Then
        assertThat(list.length,is(2));
        assertThat(list[0].getName(),is("resource1.txt"));
        assertThat(list[1].getName(),is("resource2.txt"));
    }
    @Test
    public void shouldReadAFile() throws IOException {
        //Given
        WireMockResourceRoot root = new WireMockResourceRoot(getWireMock().baseUrl(),"root");
        WireMockResourceContainer someRecordingDir = (WireMockResourceContainer) root.getChild("some_resource_dir");
        ReadableWireMockResource file= (ReadableWireMockResource) someRecordingDir.resolveExisting("resource1.txt");
        //When
        String content = new String(file.read());
        //Then
        assertThat(content,is("hello1"));
    }
    @Test
    public void shouldWriteAFile() throws IOException {
        //Given
        File rootFile = ScopedWireMockTest.getDirectoryResourceRoot().getFile();
        WireMockResourceRoot root = new WireMockResourceRoot(getWireMock().baseUrl(),"root");
        WireMockResourceContainer someRecordingDir = (WireMockResourceContainer) root.getChild("some_resource_dir");
        WritableWireMockResource file= someRecordingDir.resolvePotential("sample_output.txt");
        //When
        file.write("hello world".getBytes());
        //Then
        String found=FileUtils.readFileToString(new File(rootFile, "some_resource_dir/sample_output.txt"),"UTF-8");
        assertThat(found,is("hello world"));
    }
    @Before
    public void setup() {
        File rootFile = ScopedWireMockTest.getDirectoryResourceRoot().getFile();
        new File(rootFile, "some_resource_dir/sample_output.txt").delete();
    }
    @After
    public void cleanup() {
        File rootFile = ScopedWireMockTest.getDirectoryResourceRoot().getFile();
        new File(rootFile, "some_resource_dir/sample_output.txt").delete();
    }

    protected TestRule createWireMockRule() {
        ScopedWireMockServerRule server = new ScopedWireMockServerRule();
        server.registerResourceRoot("root", ScopedWireMockTest.getDirectoryResourceRoot());
        return server;
    }
}
