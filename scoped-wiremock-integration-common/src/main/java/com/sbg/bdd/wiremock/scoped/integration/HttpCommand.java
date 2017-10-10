package com.sbg.bdd.wiremock.scoped.integration;



import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

/**
 * An excessively lightweight command based HTTP client for HTTP operations that
 * 1. Occur only once or twice during test execution, e.g. configuration propagation
 * 2. Need to run in an environment where limiting dependencies is essential
 * NB!!!
 * No connection pooling takes place here. Use with care.
 */
public class HttpCommand {
    private static Logger LOGGER = Logger.getLogger(HttpCommand.class.getName());
    private HttpURLConnection connection;
    private URL url;
    private String method;
    private String output;

    public HttpCommand(HttpURLConnection connection, String method, String output) throws IOException {
        this.connection = connection;
        this.method = method;
        this.output = output;
    }
    public HttpCommand(URL url, String method, String output) throws IOException {
        this((HttpURLConnection) url.openConnection(), method, output);
        this.url = url;
    }

    public String execute() throws IOException {
        try {
            connection.setRequestMethod(method);
            if (output != null) {
                connection.setDoOutput(true);
                OutputStream os = connection.getOutputStream();
                os.write(output.getBytes());
                os.close();
            }
            String result = "";
            if (connection.getErrorStream() != null) {
                result = toString(connection.getErrorStream());
                LOGGER.fine(result);
            } else if (connection.getInputStream() != null) {
                result = toString(connection.getInputStream());
            }
            return result;
        } finally {
            connection.disconnect();
        }
    }

    private String toString(InputStream inputStream) throws IOException {
        InputStreamReader reader = new InputStreamReader(inputStream);
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[30];
        int chunkSize = 0;
        while((chunkSize=reader.read(buffer))>-1){
            sb.append(buffer,0,chunkSize);
        }
        return sb.toString();
    }

    public String getOutboundData() {
        return output;
    }

    public String getMethod() {
        return method;
    }

    public URL getUrl() {
        return url;
    }
}

