package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.List;

import static com.sbg.bdd.wiremock.scoped.common.Reflection.getValue;
import static com.sbg.bdd.wiremock.scoped.common.Reflection.setValue;


public class ProxyUrlTransformer extends ResponseDefinitionTransformer {


    public ProxyUrlTransformer() {
    }

    @Override
    public String getName() {
        return "ProxyUrlTransformer";
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource fileSource, Parameters parameters) {
        if (responseDefinition.isProxyResponse()) {
            String proxyUrl = calculateRelativeUrl(responseDefinition.getProxyBaseUrl(), request.getUrl(), parameters);
            return copyOf(responseDefinition, proxyUrl, request);
        } else {
            return responseDefinition;
        }


    }

    private static int getNumberOfSegments(Parameters parameters) {
        if (parameters.containsKey("numberOfSegments")) {
            return parameters.getInt("numberOfSegments");
        }  else {
            return 2;
        }
    }

    public boolean applyGlobally() {
        return false;
    }


    private static String calculateRelativeUrl(String baseUrl, String requestPath, Parameters parameters) {
        int noOfSegments = getNumberOfSegments(parameters);

        String[] split = requestPath.split("/");
        List<String> segments = new ArrayList<>();
        for (String s : split) {
            if (s.length() > 0) {
                segments.add(s);
            }
        }
        StringBuilder sb = new StringBuilder();
        if (baseUrl.charAt(baseUrl.length() - 1) != '/') {
            sb.append('/');
        }
        if (useTrailingSegments(parameters)) {
            useTrailingSegments(noOfSegments, segments, sb);
        } else if (useLeadingSegments(parameters)) {
            useLeadingSegments(noOfSegments, segments, sb);
        } else if (ignoreLeadingSegments(parameters)) {
            ignoreLeadingSegments(noOfSegments, segments, sb);
        } else if (ignoreTrailingSegments(parameters)) {
            ignoreTrailingSegments(noOfSegments, segments, sb);
        }
        return sb.toString();
    }

    private static void ignoreTrailingSegments(int noOfSegments, List<String> segments, StringBuilder sb) {
        for (int i = 0; i < segments.size() - noOfSegments; i++) {
            sb.append(segments.get(i));
            if (i < segments.size() - noOfSegments - 1) {
                sb.append('/');
            }
        }
    }

    private static void useLeadingSegments(int noOfSegments, List<String> segments, StringBuilder sb) {
        for (int i = 0; i < noOfSegments; i++) {
            sb.append(segments.get(i));
            if (i < noOfSegments - 1) {
                sb.append('/');
            }
        }
    }

    private static void useTrailingSegments(int noOfSegments, List<String> segments, StringBuilder sb) {
        for (int i = noOfSegments; i > 0; i--) {
            sb.append(segments.get(segments.size() - i));
            if (i > 1) {
                sb.append('/');
            }
        }
    }

    private static void ignoreLeadingSegments(int noOfSegments, List<String> segments, StringBuilder sb) {
        for (int i = noOfSegments; i < segments.size(); i++) {
            sb.append(segments.get(i));
            if (i < segments.size() - 1) {
                sb.append('/');
            }
        }
    }



    private static Boolean ignoreLeadingSegments(Parameters parameters) {
        return "ignore".equals(parameters.getString("action")) && "leading".equals(parameters.getString("which"));
    }

    private static Boolean useLeadingSegments(Parameters parameters) {
        return !"ignore".equals(parameters.getString("action")) && "leading".equals(parameters.getString("which"));
    }

    private static Boolean useTrailingSegments(Parameters parameters) {
        return !"ignore".equals(parameters.getString("action")) && !"leading".equals(parameters.getString("which"));
    }

    private static boolean ignoreTrailingSegments(Parameters parameters) {
        return "ignore".equals(parameters.getString("action")) && !"leading".equals(parameters.getString("which"));
    }

    private ResponseDefinition copyOf(ResponseDefinition original, final String relativeUrl, Request request) {
        ResponseDefinition result = ResponseDefinition.copyOf(original);
        HttpServletRequest httpServletRequest = getValue(request, "request");
        setValue(request, "request", new HttpServletRequestWrapper(httpServletRequest) {
            @Override
            public String getRequestURI() {
                //HACK!!!!! Only when being called from the ProxyResponseRenderer do we return the modified value.
                // For other purposes such as logging and journals we return the old value
                if (calledFromProxyRenderer()) {
                    return relativeUrl;
                } else {
                    return super.getRequestURI();
                }
            }

            private boolean calledFromProxyRenderer() {
                for (StackTraceElement s : Thread.currentThread().getStackTrace()) {
                    if (s.getClassName().contains("ProxyResponseRenderer")) {
                        return true;
                    }
                }
                return false;
            }
        });
        return result;
    }


}
