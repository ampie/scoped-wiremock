package com.sbg.bdd.wiremock.scoped;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.FatalStartupException;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.HttpsSettings;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.standalone.CommandLineOptions;
import com.github.tomakehurst.wiremock.standalone.MappingsLoader;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.stubbing.StubMappings;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.core.WireMockApp.FILES_ROOT;
import static com.github.tomakehurst.wiremock.core.WireMockApp.MAPPINGS_ROOT;
import static com.github.tomakehurst.wiremock.http.RequestMethod.ANY;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static java.lang.System.out;

public class ScopedWireMockServerRunner {

    private static final String BANNER = " /$$      /$$ /$$                     /$$      /$$                     /$$      \n" +
            "| $$  /$ | $$|__/                    | $$$    /$$$                    | $$      \n" +
            "| $$ /$$$| $$ /$$  /$$$$$$   /$$$$$$ | $$$$  /$$$$  /$$$$$$   /$$$$$$$| $$   /$$\n" +
            "| $$/$$ $$ $$| $$ /$$__  $$ /$$__  $$| $$ $$/$$ $$ /$$__  $$ /$$_____/| $$  /$$/\n" +
            "| $$$$_  $$$$| $$| $$  \\__/| $$$$$$$$| $$  $$$| $$| $$  \\ $$| $$      | $$$$$$/ \n" +
            "| $$$/ \\  $$$| $$| $$      | $$_____/| $$\\  $ | $$| $$  | $$| $$      | $$_  $$ \n" +
            "| $$/   \\  $$| $$| $$      |  $$$$$$$| $$ \\/  | $$|  $$$$$$/|  $$$$$$$| $$ \\  $$\n" +
            "|__/     \\__/|__/|__/       \\_______/|__/     |__/ \\______/  \\_______/|__/  \\__/";

    static {
        System.setProperty("org.mortbay.log.class", "com.github.tomakehurst.wiremock.jetty.LoggerAdapter");
    }

    private WireMockServer wireMockServer;

    public void run(String... args) {
        ArrayList<String> argList = new ArrayList<>(Arrays.asList(args));
        argList.add("--extensions");
        argList.add(ProxyUrlTransformer.class.getName() + "," + ScopeExtensions.class.getName() + "," + InvalidHeadersLoggingTransformer.class.getName());
        final CommandLineOptions options = new CommandLineOptions(argList.toArray(new String[0]));
        if (options.help()) {
            out.println(options.helpText());
            return;
        }
        FileSource fileSource = options.filesRoot();
        fileSource.createIfNecessary();
        FileSource filesFileSource = fileSource.child(FILES_ROOT);
        filesFileSource.createIfNecessary();
        FileSource mappingsFileSource = fileSource.child(MAPPINGS_ROOT);
        mappingsFileSource.createIfNecessary();
        if(System.getProperty("javax.net.ssl.keyStore")!=null) {
            Options proxyOptions = (Options) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Options.class}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("httpsSettings")) {
                        return new HttpsSettings.Builder().trustStorePath(System.getProperty("javax.net.ssl.keyStore")).trustStorePassword(System.getProperty("javax.net.ssl.keyStorePassword")).build();
                    } else {
                        return method.invoke(options,args);
                    }
                }
            });
            wireMockServer = new ScopedWireMockServer(proxyOptions);
        }else{
            wireMockServer = new ScopedWireMockServer(options);
        }
        if (options.recordMappingsEnabled()) {
            wireMockServer.enableRecordMappings(mappingsFileSource, filesFileSource);
        }

        if (options.specifiesProxyUrl()) {
            addProxyMapping(options.proxyUrl());
        }

        try {
            wireMockServer.start();
            out.println(BANNER);
            out.println();
            out.println(options);
        } catch (FatalStartupException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private void addProxyMapping(final String baseUrl) {
        wireMockServer.loadMappingsUsing(new MappingsLoader() {
            @Override
            public void loadMappingsInto(StubMappings stubMappings) {
                RequestPattern requestPattern = newRequestPattern(ANY, anyUrl()).build();
                ResponseDefinition responseDef = responseDefinition()
                        .proxiedFrom(baseUrl)
                        .build();

                StubMapping proxyBasedMapping = new StubMapping(requestPattern, responseDef);
                proxyBasedMapping.setPriority(10); // Make it low priority so that existing stubs will take precedence
                stubMappings.addMapping(proxyBasedMapping);
            }
        });
    }

    public void stop() {
        wireMockServer.stop();
    }

    public boolean isRunning() {
        return wireMockServer.isRunning();
    }

    public int port() {
        return wireMockServer.port();
    }

    public static void main(String... args) {
        new ScopedWireMockServerRunner().run(args);
    }
}
