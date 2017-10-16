package com.sbg.bdd.wiremock.scoped.filter;

import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class InboundCorrelationPathFilter implements Filter {
    static final Logger LOGGER = Logger.getLogger(InboundCorrelationPathFilter.class.getName());
    public static final String SCOPED_WIREMOCK_ENABLED = InboundCorrelationPathFilter.class.getSimpleName() + ".scoped_wiremock_enabled";

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
            if (processEndPointConfigRequest(response, request)) {
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

    private boolean processEndPointConfigRequest(ServletResponse response, HttpServletRequest request) throws IOException {
        int indexOf = request.getRequestURI().indexOf(EndpointConfig.ENDPOINT_CONFIG_PATH +"all");
        if (indexOf > 1) {
            response.setContentType("application/json");
            ServletOutputStream outputStream = response.getOutputStream();
            return writeAllEndpointConfigs(outputStream);
        }
        return false;
    }

    private boolean writeAllEndpointConfigs(ServletOutputStream outputStream) throws IOException {
        outputStream.print("[");
        Set<EndpointConfig> endpointProperties = ServerSideEndPointConfigRegistry.getInstance().getAllEndpointConfigs();
        Iterator<EndpointConfig> iterator = endpointProperties.iterator();
        while (iterator.hasNext()) {
            outputStream.print(iterator.next().toJson());
            if (iterator.hasNext()) {
                outputStream.print(",");
            }
        }
        outputStream.print("]");
        return true;
    }

    @Override
    public void destroy() {

    }

}
