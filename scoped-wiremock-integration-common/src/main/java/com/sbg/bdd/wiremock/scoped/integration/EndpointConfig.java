package com.sbg.bdd.wiremock.scoped.integration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EndpointConfig implements Comparable<EndpointConfig> {
    public static final String ENDPOINT_CONFIG_PATH = "/__EndpointConfig/";
    public static final String LOCAL_INTEGRATION_SCOPE = "local";

    private static final String FORMAT = "{\"propertyName\":\"%s\",\"url\":\"%s\",\"endpointType\":\"%s\",\"categories\":[%s],\"scopes\":[%s]}";
    private static final String PARSE_STRING = buildParseString();

    public enum EndpointType {
        SOAP, UNKOWN, REST
    }

    private final List<String> categories;
    private final List<String> scopes;


    private String propertyName;
    private URL url;
    private EndpointType endpointType;

    public EndpointConfig(String propertyName, EndpointType endpointType, String[] categories, String[] scopes) {
        this.propertyName = propertyName;
        this.endpointType = endpointType;
        this.categories = Arrays.asList(categories);
        this.scopes = Arrays.asList(scopes);
    }

    public EndpointConfig(String propertyName, EndpointType endpointType, String... categories) {
        this(propertyName, endpointType, categories, new String['0']);
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public List<String> getCategories() {
        return categories;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public URL getUrl() {
        return url;
    }

    public EndpointType getEndpointType() {
        return endpointType;
    }

    public String toJson() {
        //just code it here - it is simple enough
        return String.format(
                FORMAT,
                propertyName, url.toExternalForm(), endpointType.name(), toString(categories), toString(scopes));

    }

    public static EndpointConfig oneFromJson(String json) {
        //OK, not so simple, but we don't want external dependencies here
        try {
            Pattern compile = Pattern.compile(PARSE_STRING);
            Matcher matcher = compile.matcher(json);
            if (matcher.find() && matcher.groupCount() == 5) {
                EndpointConfig config = new EndpointConfig(matcher.group(1), EndpointType.valueOf(matcher.group(3)), toArray(matcher, 4), toArray(matcher, 5));
                config.setUrl(new URL(matcher.group(2)));
                return config;
            } else {
                throw new IllegalArgumentException("Invalid EndPointConfig json: " + json);
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static EndpointConfig[] manyFromJson(String json) {
        if (json.length() > 2) {
            String[] split = json.substring(2, json.length() - 2).split("\\},\\{");
            EndpointConfig[] result = new EndpointConfig[split.length];
            for (int i = 0; i < result.length; i++) {
                result[0] = oneFromJson("{" + split[i] + "}");
            }
            return result;
        } else {
            return new EndpointConfig[0];
        }
    }

    private static String buildParseString() {
        String[] toEscape = {"{", "}", "[", "]", "\"", ","};
        String escaped = FORMAT.replace("%s", "(.*)");
        for (String c : toEscape) {
            escaped = escaped.replace(c, "\\" + c);
        }
        return escaped;
    }

    private static String[] toArray(Matcher matcher, int i) {
        return matcher.group(i).replaceAll("\"", "").split(",");
    }

    private String toString(List<String> elements) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < elements.size(); i++) {
            sb = sb.append("\"").append(elements.get(i)).append("\"");
            if (i < elements.size() - 1) {
                sb = sb.append(",");
            }
        }
        return sb.toString();
    }

    @Override
    public int compareTo(EndpointConfig o) {
        return propertyName.compareTo(o.propertyName);
    }
}
