//package com.sbg.bdd.wiremock.scoped.jaxws;
//
//import com.examples.wsdl.helloservice_wsdl.HelloPortType;
//import com.examples.wsdl.helloservice_wsdl.HelloService;
//import com.github.tomakehurst.wiremock.WireMockServer;
//import com.github.tomakehurst.wiremock.client.WireMock;
//import com.github.tomakehurst.wiremock.core.Options;
//import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
//import com.sbg.bdd.wiremock.scoped.integration.*;
//import com.sbg.bdd.wiremock.scoped.integration.*;
//import org.jboss.arquillian.container.test.api.Deployment;
//import org.jboss.arquillian.container.test.api.RunAsClient;
//import org.jboss.arquillian.junit.Arquillian;
//import org.jboss.shrinkwrap.api.ShrinkWrap;
//import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
//import org.jboss.shrinkwrap.api.asset.EmptyAsset;
//import org.jboss.shrinkwrap.api.asset.StringAsset;
//import org.jboss.shrinkwrap.api.spec.WebArchive;
//import org.junit.AfterClass;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import javax.ejb.EJB;
//import java.net.URL;
//import java.util.Map;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
//import static com.github.tomakehurst.wiremock.client.WireMock.post;
//import static org.hamcrest.core.Is.is;
//import static org.hamcrest.core.IsEqual.equalTo;
//import static org.junit.Assert.assertThat;
//
////@RunWith(Arquillian.class)
//public class WhenCallingAnInterceptedSoapOperation {
//    private static Object wireMock;
//
////    @RunAsClient
////    @AfterClass
//    public static void clenaup() {
//        ((WireMockServer) wireMock).shutdown();
//    }
//
//    @Deployment
//    public static WebArchive createDeployment() {
//        WhenCallingAnInterceptedSoapOperation.wireMock = new WireMockServer(Options.DYNAMIC_PORT);
//        WireMockServer wireMock = (WireMockServer) WhenCallingAnInterceptedSoapOperation.wireMock;
//        wireMock.start();
//        wireMock.addStubMapping(post("/dummy")
//                .withHeader(HeaderName.ofTheCorrelationKey(), WireMock.equalTo("localhost/9999/myscope"))
//                .withHeader(HeaderName.ofTheSequenceNumber(), WireMock.equalTo("1"))
////                .withHeader(, MultiValuePattern.of(WireMock.equalTo()))
//                .willReturn(aResponse().withBody(
//                        "<SOAP-ENV:Envelope\n" +
//                                "        xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
//                                "    <SOAP-ENV:Body>\n" +
//                                "        <greeting xmlns=\"http://www.examples.com/wsdl/HelloService.wsdl\">" +
//                                "matched!" +
//                                "</greeting>\n" +
//                                "    </SOAP-ENV:Body>\n" +
//                                "</SOAP-ENV:Envelope>")
//                        .withHeader(ContentTypeHeader.KEY, "application/xml")
//                        .withHeader(HeaderName.ofTheServiceInvocationCount(), "som-arb-service|12")).build());
//        return ShrinkWrap.create(WebArchive.class)
//                .addClass(OutboundCorrelationPathSOAPHandler.class)
//                .addClass(HelloWorldConsumer.class)
//                .addClass(HelloService.class)
//                .addClass(DependencyInjectionAdaptorFactory.class)
//                .addClass(DependencyInjectorAdaptor.class)
//                .addClass(HeaderName.class)
//                .addClass(BaseDependencyInjectorAdaptor.class)
//                .addClass(BaseWireMockCorrelationState.class)
//                .addClass(EndPointRegistry.class)
//                .addClass(WireMockCorrelationState.class)
//                .addAsResource(new StringAsset("my.endpoint=http://localhost:" + wireMock.port() + "/dummy"), "endpoints.properties")
//                .addAsManifestResource(new ClassLoaderAsset("HelloService.wsdl"), "HelloService.wsdl")
//                .addClass(HelloPortType.class)
//                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
//    }
//
//    @EJB
//    private HelloWorldConsumer consumer;
//
////    @Test
//    public void itShouldIncrementTheInvocationCount() throws Exception {
//
//        WireMockCorrelationState state = BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE = new BaseWireMockCorrelationState();
//        state.set("localhost/9999/myscope", false);
//        state.initSequenceNumberFor("som-arb-service", 5);
//        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor());
//        assertThat(consumer.callHello(), is(equalTo("matched!")));
//        assertThat(state.getSequenceNumbers().get("{http://www.examples.com/wsdl/HelloService.wsdl}sayHello"), is(1));
//        assertThat(state.getSequenceNumbers().get("som-arb-service"), is(12));
//    }
//}
//
//
//
