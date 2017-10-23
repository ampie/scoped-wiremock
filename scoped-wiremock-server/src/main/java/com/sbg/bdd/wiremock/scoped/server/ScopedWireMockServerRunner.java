package com.sbg.bdd.wiremock.scoped.server;

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
import com.sbg.bdd.resource.file.DirectoryResourceRoot;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

    private static ScopedWireMockServer wireMockServer;

    public static ScopedWireMockServer getWireMockServer() {
        return wireMockServer;
    }

    public void run(String... args) {
        ArrayList<String> argList = new ArrayList<>();
        Map<String, DirectoryResourceRoot> resourceRoots = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--resourceRoot")) {
                String[] split = args[i + 1].split("\\:");
                resourceRoots.put(split[0], new DirectoryResourceRoot(split[0], new File(split[1])));
                i++;
            } else if (arg.equals("--extensions")) {
                argList.add(arg);
                argList.add(args[i + 1] + "," + buildExtensionsString());
                i++;
            } else {
                argList.add(arg);
            }
        }
        if (!argList.contains("--port")) {
            argList.add("--port");
            argList.add("0");
        }
        if (!argList.contains("--extensions")) {
            argList.add("--extensions");
            String extensions = buildExtensionsString();
            argList.add(extensions);
        }
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
        if (System.getProperty("javax.net.ssl.keyStore") != null) {
            Options proxyOptions = (Options) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Options.class}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("httpsSettings")) {
                        return new HttpsSettings.Builder().trustStorePath(System.getProperty("javax.net.ssl.keyStore")).trustStorePassword(System.getProperty("javax.net.ssl.keyStorePassword")).build();
                    } else {
                        return method.invoke(options, args);
                    }
                }
            });
            wireMockServer = new ScopedWireMockServer(proxyOptions);
        } else {
            wireMockServer = new ScopedWireMockServer(options);
        }
        if (options.recordMappingsEnabled()) {
            wireMockServer.enableRecordMappings(mappingsFileSource, filesFileSource);
        }

        if (options.specifiesProxyUrl()) {
            throw new IllegalArgumentException("Global proxy urls not supported in ScopedWireMock");
        }

        try {
            for (Map.Entry<String, DirectoryResourceRoot> entry : resourceRoots.entrySet()) {
                wireMockServer.registerResourceRoot(entry.getKey(), entry.getValue());
            }
            wireMockServer.start();
            out.println(BANNER);
            out.println();
            out.println(options);
        } catch (FatalStartupException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private String buildExtensionsString() {
        String extensions = ProxyUrlTransformer.class.getName() + "," +
                ScopeExtensions.class.getName() + "," +
                InvalidHeadersLoggingTransformer.class.getName() + "," +
                ScopeUpdatingResponseTransformer.class.getName();
        try {
            Class<?> cls = Class.forName("com.sbg.bdd.wiremock.scoped.integration.cucumber.CucumberFormattingScopeListener");
            extensions = extensions + "," + cls.getName();
        } catch (ClassNotFoundException e) {

        }
        return extensions;
    }


    public static void main(String... args) {
        new ScopedWireMockServerRunner().run(args);
    }
}
