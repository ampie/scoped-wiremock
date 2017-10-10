package com.sbg.bdd.wiremock.scoped.cdi;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * NB!!! Generated initially, but modified for tests to return the DummyBinding
 */
@WebServiceClient(name = "Hello_Service",
                  wsdlLocation = "file:/home/ampie/Code/ampie/scoped-wiremock/scoped-wiremock-integration-jaxws/src/test/resources/HelloService.wsdl",
                  targetNamespace = "http://www.examples.com/wsdl/HelloService.wsdl")
public class HelloService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://www.examples.com/wsdl/HelloService.wsdl", "Hello_Service");
    public final static QName HelloPort = new QName("http://www.examples.com/wsdl/HelloService.wsdl", "Hello_Port");
    static {
        URL url = null;
            url = HelloService.class.getClassLoader().getResource("HelloService.wsdl");
        WSDL_LOCATION = url;
    }

    public HelloService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public HelloService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public HelloService() {
        super(WSDL_LOCATION, SERVICE);
    }

    public HelloService(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public HelloService(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public HelloService(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }
    public <T> T getPort(QName portName, Class<T> serviceEndpointInterface, WebServiceFeature... features) {
        return (T) new DummyBinding();
    }

    public <T> T getPort(QName portName, Class<T> serviceEndpointInterface) {
        return (T) new DummyBinding();
    }
    public <T> T getPort(Class<T> itf) {
        return (T) new DummyBinding();
    }



    @WebEndpoint(name = "Hello_Port")
    public HelloPortType getHelloPort() {
        return getPort(HelloPort, HelloPortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns HelloPortType
     */
    @WebEndpoint(name = "Hello_Port")
    public HelloPortType getHelloPort(WebServiceFeature... features) {
        return getPort(HelloPort, HelloPortType.class, features);
    }

}
