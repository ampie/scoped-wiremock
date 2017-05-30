package com.sbg.bdd.wiremock.scoped.recording

import com.github.tomakehurst.wiremock.http.ContentTypeHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.sbg.bdd.resource.file.RootDirectoryResource
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedRequest
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedResponse
import com.sbg.bdd.wiremock.scoped.recording.strategies.RecordingStrategies
import com.sbg.bdd.wiremock.scoped.server.ScopedWireMockServer
import org.apache.commons.io.FileUtils

import java.nio.file.Files
import java.nio.file.Paths

import static com.github.tomakehurst.wiremock.client.WireMock.matching
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import static com.github.tomakehurst.wiremock.http.RequestMethod.PUT
import static com.sbg.bdd.wiremock.scoped.recording.strategies.RequestStrategies.anyRequest

class WhenRecordingResponses extends WhenWorkingWithWireMock{

    def 'should record both response body and response header files in the specified directory'() throws Exception{
        given:
        def outputDir = new RootDirectoryResource(Files.createTempDirectory("scoped-wiremock-playback").toFile())
        def wireMockServer = Mock(ScopedWireMockServer){
            findMatchingExchanges(_) >> {
                def request1 = new RecordedRequest()
                request1.requestedUrl = 'http://somehost/context/service/operation1'
                request1.method=RequestMethod.GET
                def request2 = new RecordedRequest()
                request2.requestedUrl = 'http://somehost/context/service/operation2'
                request2.method=RequestMethod.PUT;
                def exchange1 = new RecordedExchange(request1, 'nested1', null)
                def response1 = new RecordedResponse()
                response1.status = 1
                response1.base64Body =new String(Base64.encoder.encode("{\"name\"=\"value\"}".bytes))
                response1.headers = new HttpHeaders().plus(new ContentTypeHeader('application/json'))
                exchange1.recordResponse(response1)
                def exchange2 = new RecordedExchange(request2, 'nested1', null)
                def response2 = new RecordedResponse()
                response2.headers = new HttpHeaders().plus(new ContentTypeHeader('application/xml'))
                response2.base64Body =new String(Base64.encoder.encode('<root/>'.bytes))
                exchange2.recordResponse(response2)
                return [exchange1, exchange2]
            }
        }
        def recordingWireMockClient = new RecordingWireMockClient(wireMockServer)
        when:
            recordingWireMockClient.saveRecordingsForRequestPattern(
                    matching('/scopepath/.'),
                    anyRequest().to('/context.*').build(),
                    outputDir
            )

        then:
        outputDir.resolveExisting('service_GET_operation1_0.json').read() == "{\"name\"=\"value\"}".bytes
        outputDir.resolveExisting('service_GET_operation1_0.headers.json') != null
        outputDir.resolveExisting('service_PUT_operation2_0.xml').read() == "<root/>".bytes
    }


}
