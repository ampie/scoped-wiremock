package com.sbg.bdd.wiremock.scoped.filter;

import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class InboundCorrelationPathFilter implements Filter {
    static final Logger LOGGER = Logger.getLogger(InboundCorrelationPathFilter.class.getName());
    private static final String PROPERTY_PATH = "/Property/";
    public static final java.lang.String SCOPED_WIREMOCK_ENABLED = InboundCorrelationPathFilter.class.getSimpleName() + "scoped_wiremock_enabled";

    private PropertyWriter propertyWriter;


    private EndPointRegistry endpointRegistry;


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        endpointRegistry = DependencyInjectionAdaptorFactory.getAdaptor().getEndpointRegistry();
        propertyWriter = new PropertyWriter(endpointRegistry);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        CorrelationStateSynchronizer correlationStateSynchronizer = new CorrelationStateSynchronizer(endpointRegistry);
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
            boolean resolveIp = "true".equals(request.getParameter("resolveIp"));
            return propertyWriter.maybeWriteOneOrAllProperties(response.getOutputStream(), propertyName, resolveIp);
        }
        return false;
    }


    @Override
    public void destroy() {

    }

}
