package com.sbg.bdd.wiremock.scoped.jaxws;

import com.examples.wsdl.helloservice_wsdl.HelloPortType;
import com.examples.wsdl.helloservice_wsdl.HelloService;

import javax.ejb.Stateless;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.handler.Handler;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

@Stateless
public class HelloWorldConsumer {
    @WebServiceRef(wsdlLocation="META-INF/HelloService.wsdl")
    HelloService helloService;

    public String callHello() throws IOException {
        Properties props = new Properties();
        props.load(HelloWorldConsumer.class.getResourceAsStream("/endpoints.properties"));
        HelloPortType helloPort = helloService.getHelloPort();
        BindingProvider helloPort1 = (BindingProvider) helloPort;
        Binding binding = helloPort1.getBinding();
        helloPort1.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, props.getProperty("my.endpoint"));
        binding.setHandlerChain(Arrays.<Handler>asList(new OutboundCorrelationPathSOAPHandler()));
        return helloPort.sayHello("John");
    }
}
