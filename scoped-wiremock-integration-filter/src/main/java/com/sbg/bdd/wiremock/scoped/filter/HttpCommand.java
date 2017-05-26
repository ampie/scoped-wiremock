package com.sbg.bdd.wiremock.scoped.filter;



import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpCommand {
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
                InboundCorrelationPathFilter.LOGGER.fine(result);
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

    public String getOutput() {
        return output;
    }

    public String getMethod() {
        return method;
    }

    public URL getUrl() {
        return url;
    }
}

