package com.sbg.bdd.wiremock.scoped.client.strategies;


import com.sbg.bdd.wiremock.scoped.client.WireMockContext;
import com.sbg.bdd.wiremock.scoped.client.builders.ExtendedMappingBuilder;
import com.sbg.bdd.wiremock.scoped.client.builders.ExtendedResponseDefinitionBuilder;
import com.sbg.bdd.wiremock.scoped.client.builders.ResponseStrategy;

import static java.lang.String.format;

public abstract class RecordingStrategies {
    public static ResponseStrategy mapToJournalDirectory(final String journalDirectoryOverride) {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception {
                builder.getRecordingSpecification().mapsToJournalDirectory(journalDirectoryOverride);
                builder.getRequestPatternBuilder().changeUrlToPattern();
                builder.atPriority(scope.calculatePriority(1));
                return null;
            }

            @Override
            public String getDescription() {
                return format("map to journal directory \"%s\"",journalDirectoryOverride);
            }
        };
    }

    public static ResponseStrategy mapToJournalDirectory() {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception {
                builder.getRecordingSpecification().mapsToJournalDirectory();
                builder.getRequestPatternBuilder().changeUrlToPattern();
                builder.atPriority(scope.calculatePriority(1));
                return null;
            }
            @Override
            public String getDescription() {
                return format("map to global journal directory");
            }

        };
    }


    public static ResponseStrategy playbackResponsesFrom(final String recordingDirectory) {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception {
                builder.getRequestPatternBuilder().changeUrlToPattern();
                builder.getRecordingSpecification().playbackResponsesFrom(recordingDirectory);
                builder.atPriority(scope.calculatePriority(2));
                return null;
            }

            @Override
            public String getDescription() {
                return format("playback responses from  \"%s\"",recordingDirectory);
            }
        };
    }

    public static ResponseStrategy playbackResponses() {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception {
                builder.getRequestPatternBuilder().changeUrlToPattern();
                builder.getRecordingSpecification().playbackResponses();
                builder.atPriority(scope.calculatePriority(2));
                return null;
            }
            @Override
            public String getDescription() {
                return format("playback responses from the current resource directory");
            }
        };
    }

    public static ResponseStrategy recordResponsesTo(final String recordingDirectory) {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception {
                builder.getRequestPatternBuilder().changeUrlToPattern();
                builder.getRecordingSpecification().recordingResponsesTo(recordingDirectory);
                builder.atPriority(scope.calculatePriority(2));
                return null;
            }
            @Override
            public String getDescription() {
                return format("record responses to \"%s\"",recordingDirectory);
            }
        };
    }

    public static ResponseStrategy recordResponses() {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception {
                builder.getRequestPatternBuilder().changeUrlToPattern();
                builder.getRecordingSpecification().recordingResponses();
                builder.atPriority(scope.calculatePriority(2));
                return null;
            }
            @Override
            public String getDescription() {
                return format("record responses to the current resource directory");
            }
        };
    }
}
