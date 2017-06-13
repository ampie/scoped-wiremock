package com.sbg.bdd.wiremock.scoped;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.file.DirectoryResourceRoot;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedRequest;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedResponse;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import java.nio.file.Files;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

abstract class WhenRecordingResponses extends ScopedWireMockTest {
    ///MMM??? we are also going to have to add the headers and crap. Ag tog.
    protected abstract void recordExchanges(List<RecordedExchange> exchanges);
    @Test
    public void should_record_both_response_body_and_response_header_files_in_the_specified_directory() throws Exception {
        DirectoryResourceRoot outputDir = new DirectoryResourceRoot("root", Files.createTempDirectory("scoped-wiremock-playback").toFile());
//        def wireMockServer = Mock(ScopedWireMockServer) {
//            findMatchingExchanges(_) >> {
//                    def request1 = new RecordedRequest()
//                    request1.requestedUrl = "http://somehost/context/service/operation1"
//                    request1.method = RequestMethod.GET
//                    def request2 = new RecordedRequest()
//                    request2.requestedUrl = "http://somehost/context/service/operation2"
//                    request2.method = RequestMethod.PUT;
//            def exchange1 = new RecordedExchange(request1, "nested1", null)
//            def response1 = new RecordedResponse()
//            response1.status = 1
//            response1.base64Body = new String(Base64.encoder.encode("{\"name\"=\"value\"}".getBytes()))
//            response1.headers = new HttpHeaders().plus(new ContentTypeHeader("application/json"))
//            exchange1.recordResponse(response1)
//            def exchange2 = new RecordedExchange(request2, "nested1", null)
//            def response2 = new RecordedResponse()
//            response2.headers = new HttpHeaders().plus(new ContentTypeHeader("application/xml"))
//            response2.base64Body = new String(Base64.encode("<root/>".getBytes()))
//            exchange2.recordResponse(response2)
//            return[exchange1, exchange2]
//            }
//        }
        getWireMock().saveRecordingsForRequestPattern(
                matching("/scopepath/."),
                WireMock.any(WireMock.urlPathMatching("/context.*")).build().getRequest(),
                outputDir
        );

        assertThat(((ReadableResource)outputDir.resolveExisting("service_GET_operation1_0.json")).read(), is("{\"name\"=\"value\"}".getBytes()));
        assertThat(outputDir.resolveExisting("service_GET_operation1_0.headers.json"), is(not(nullValue())));
        assertThat(((ReadableResource)outputDir.resolveExisting("service_PUT_operation2_0.xml")).read(), is("<root/>".getBytes()));
    }


}
