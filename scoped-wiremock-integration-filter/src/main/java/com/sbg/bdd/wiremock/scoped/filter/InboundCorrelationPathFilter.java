package com.sbg.bdd.wiremock.scoped.filter;

import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class InboundCorrelationPathFilter implements Filter {
    static final Logger LOGGER = Logger.getLogger(InboundCorrelationPathFilter.class.getName());
    private static final String PROPERTY_PATH = "/Property/";
    public static final String SCOPED_WIREMOCK_ENABLED = InboundCorrelationPathFilter.class.getSimpleName() + "scoped_wiremock_enabled";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        CorrelationStateSynchronizer correlationStateSynchronizer = new CorrelationStateSynchronizer();
        correlationStateSynchronizer.clearCorrelationSession();
        HttpServletRequest request = (HttpServletRequest) req;
        try {
            boolean doFilter = true;
            if (processPropertyRequest(response, request)) {
                doFilter = false;
            } else if (isAutomationSupportEnabled()) {
                correlationStateSynchronizer.readCorrelationSessionFrom(request);
            }
            if (doFilter) {
                chain.doFilter(request, response);
            }
        } finally {
            correlationStateSynchronizer.maybeWriteCorrelationSessionTo((HttpServletResponse) response);
            correlationStateSynchronizer.clearCorrelationSession();
        }
    }

    private boolean isAutomationSupportEnabled() {
        return "true".equals(System.getProperty(SCOPED_WIREMOCK_ENABLED));
    }

    private boolean processPropertyRequest(ServletResponse response, HttpServletRequest request) throws IOException {
        int indexOf = request.getRequestURI().indexOf(PROPERTY_PATH);
        if (indexOf > 1) {
            response.setContentType("application/json");
            String propertyName = request.getRequestURI().substring(indexOf + PROPERTY_PATH.length());
            ServletOutputStream outputStream = response.getOutputStream();
            if (propertyName.equals("all")) {
                return writeAllEndpointConfigs(outputStream);
            } else {
                return writeSingleEndpointConfig(outputStream, propertyName);
            }
        }
        return false;
    }

    private boolean writeSingleEndpointConfig(ServletOutputStream outputStream, String propertyName) throws IOException {
        EndpointConfig config = EndpointTypeTracker.getInstance().getEndpointConfig(propertyName);
        if (config != null) {
            outputStream.print(config.toJson());
            return true;
        }
        return false;
    }

    private boolean writeAllEndpointConfigs(ServletOutputStream outputStream) throws IOException {
        outputStream.print("{\"configs\":[");
        Set<EndpointConfig> endpointProperties = EndpointTypeTracker.getInstance().getAllEndpointConfigs();
        Iterator<EndpointConfig> iterator = endpointProperties.iterator();
        while (iterator.hasNext()) {
            outputStream.print(iterator.next().toJson());
            if (iterator.hasNext()) {
                outputStream.print(",");
            }
        }
        outputStream.print("]}");
        return true;
    }

    @Override
    public void destroy() {

    }

}
