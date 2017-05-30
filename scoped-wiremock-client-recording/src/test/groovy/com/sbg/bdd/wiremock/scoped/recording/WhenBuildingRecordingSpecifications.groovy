package com.sbg.bdd.wiremock.scoped.recording

import com.sbg.bdd.wiremock.scoped.recording.builders.JournalMode
import com.sbg.bdd.wiremock.scoped.recording.strategies.RecordingStrategies

import java.nio.file.Paths

import static com.github.tomakehurst.wiremock.http.RequestMethod.PUT
import static com.sbg.bdd.wiremock.scoped.recording.strategies.RecordingStrategies.mapToJournalDirectory
import static com.sbg.bdd.wiremock.scoped.recording.strategies.RecordingStrategies.playbackResponses
import static com.sbg.bdd.wiremock.scoped.recording.strategies.RecordingStrategies.playbackResponsesFrom
import static com.sbg.bdd.wiremock.scoped.recording.strategies.RecordingStrategies.recordResponses
import static com.sbg.bdd.wiremock.scoped.recording.strategies.RecordingStrategies.recordResponsesTo
import static com.sbg.bdd.wiremock.scoped.recording.strategies.RequestStrategies.a

class WhenBuildingRecordingSpecifications extends WhenWorkingWithWireMock {

    def 'should associate a recording mapping with the current ActorInScope wihtout registering on WireMock'() throws Exception {

        given:
        def wireMockContext = initializeWireMockContext()
        def tempDir = File.createTempDir("wiremock-screenplay-tests-WhenBuildingRecordingSpecifications", "")

        when:

        a(PUT).to("/home/path").to(recordResponsesTo(tempDir.absolutePath)).applyTo(wireMockContext)


        then:
        def mappings = wireMockContext.mappings
        mappings.size() == 0
        def requestsToRecord = wireMockContext.recordingMappingBuilders
        requestsToRecord.size() == 1
        requestsToRecord[0].recordingSpecification.journalModeOverride == JournalMode.RECORD
        requestsToRecord[0].recordingSpecification.recordToCurrentResourceDir() == false
        requestsToRecord[0].recordingSpecification.enforceJournalModeInScope() == false
        requestsToRecord[0].recordingSpecification.recordingDirectory == tempDir.absolutePath
    }

    def 'should record responses to the current output resource directory when no path is specified'() throws Exception {
        given:
        def wireMockContext = initializeWireMockContext()

        when:

        a(PUT).to("/home/path").will(recordResponses()).applyTo(wireMockContext)

        then:
        def mappings = wireMockContext.mappings
        mappings.size() == 0
        def requestsToRecord = wireMockContext.recordingMappingBuilders
        requestsToRecord.size() == 1
        requestsToRecord[0].recordingSpecification.journalModeOverride == JournalMode.RECORD
        requestsToRecord[0].recordingSpecification.recordToCurrentResourceDir() == true
        requestsToRecord[0].recordingSpecification.enforceJournalModeInScope() == false
        requestsToRecord[0].recordingSpecification.recordingDirectory == null
    }

    def 'should playback responses from the current input resource directory when no path is specified'() throws Exception {
        given:
        def wireMockContext = initializeWireMockContext()

        when:

        a(PUT).to("/home/path").to(playbackResponses()).applyTo(wireMockContext)

        then:
        def mappings = wireMockContext.mappings
        mappings.size() == 0
        def requestsToRecord = wireMockContext.recordingMappingBuilders
        requestsToRecord.size() == 1
        requestsToRecord[0].recordingSpecification.journalModeOverride == JournalMode.PLAYBACK
        requestsToRecord[0].recordingSpecification.recordToCurrentResourceDir() == true
        requestsToRecord[0].recordingSpecification.enforceJournalModeInScope() == false
        requestsToRecord[0].recordingSpecification.recordingDirectory == null
    }
    def 'should playback responses from the specified directory'() throws Exception {
        given:
        def wireMockContext = initializeWireMockContext()

        when:

        a(PUT).to("/home/path").to(playbackResponsesFrom('some/dir')).applyTo(wireMockContext)

        then:
        def mappings = wireMockContext.mappings
        mappings.size() == 0
        def requestsToRecord = wireMockContext.recordingMappingBuilders
        requestsToRecord.size() == 1
        requestsToRecord[0].recordingSpecification.journalModeOverride == JournalMode.PLAYBACK
        requestsToRecord[0].recordingSpecification.recordToCurrentResourceDir() == false
        requestsToRecord[0].recordingSpecification.enforceJournalModeInScope() == false
        requestsToRecord[0].recordingSpecification.recordingDirectory == 'some/dir'
    }

    def 'should map responses to the current journal directory when no path is specified'() throws Exception {
        given:
        def wireMockContext = initializeWireMockContext()
        when:

        a(PUT).to("/endpoint/path").to(mapToJournalDirectory()).applyTo(wireMockContext)

        then:
        def mappings = wireMockContext.mappings
        mappings.size() == 0
        def requestsToRecord = wireMockContext.recordingMappingBuilders
        requestsToRecord.size() == 1
        requestsToRecord[0].recordingSpecification.enforceJournalModeInScope() == true
        requestsToRecord[0].recordingSpecification.recordToCurrentResourceDir() == true
        requestsToRecord[0].recordingSpecification.journalModeOverride == null
        requestsToRecord[0].recordingSpecification.recordingDirectory == null
    }

    def 'should map responses to the current resource directory under the journal directory when no path is specified'() throws Exception {
        given:
        def wireMockContext = initializeWireMockContext()

        when:

        a(PUT).to("/home/path").to(mapToJournalDirectory("/tmp/journal")).applyTo(wireMockContext)

        then:
        def mappings = wireMockContext.mappings
        mappings.size() == 0
        def requestsToRecord = wireMockContext.recordingMappingBuilders
        requestsToRecord.size() == 1
        requestsToRecord[0].recordingSpecification.enforceJournalModeInScope() == true
        requestsToRecord[0].recordingSpecification.recordToCurrentResourceDir() == false
        requestsToRecord[0].recordingSpecification.journalModeOverride == null
        requestsToRecord[0].recordingSpecification.recordingDirectory == '/tmp/journal'
    }

}