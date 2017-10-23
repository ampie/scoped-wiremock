package com.sbg.bdd.wiremock.scoped.admin.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "RecordingSpecification",
        propOrder = {"recordToCurrentResourceDir", "enforceJournalModeInScope", "journalModeOverride", "recordingDirectory"}
)
public class RecordingSpecification {
    @XmlElement(
    )
    private boolean recordToCurrentResourceDir;

    @XmlElement(
    )
    private boolean enforceJournalModeInScope = false;
    @XmlElement(
            nillable = true
    )
    private JournalMode journalModeOverride;
    @XmlElement(
            nillable = true
    )
    private String recordingDirectory;

    public RecordingSpecification() {

    }

    public RecordingSpecification(RecordingSpecification source) {
        this.journalModeOverride = source.journalModeOverride;
        this.recordingDirectory = source.recordingDirectory;
        this.enforceJournalModeInScope = source.enforceJournalModeInScope;
        this.recordToCurrentResourceDir = source.recordToCurrentResourceDir;

    }

    public boolean recordToCurrentResourceDir() {
        return recordToCurrentResourceDir;
    }

    public boolean enforceJournalModeInScope() {
        return enforceJournalModeInScope;
    }

    public RecordingSpecification recordingResponses() {
        recordToCurrentResourceDir = true;
        journalModeOverride = JournalMode.RECORD;
        return this;
    }

    public RecordingSpecification recordingResponsesTo(String directory) {
        this.recordingDirectory = directory;
        recordToCurrentResourceDir = false;
        journalModeOverride = JournalMode.RECORD;
        return this;
    }

    public RecordingSpecification playbackResponses() {
        recordToCurrentResourceDir = true;
        journalModeOverride = JournalMode.PLAYBACK;
        return this;
    }

    public RecordingSpecification playbackResponsesFrom(String directory) {
        this.recordingDirectory = directory;
        recordToCurrentResourceDir = false;
        journalModeOverride = JournalMode.PLAYBACK;
        return this;
    }

    public RecordingSpecification mapsToJournalDirectory(String journalDirectoryOverride) {
        this.recordingDirectory = journalDirectoryOverride;
        recordToCurrentResourceDir = false;
        enforceJournalModeInScope = true;
        return this;
    }


    public JournalMode getJournalModeOverride() {
        return journalModeOverride;
    }

    public String getRecordingDirectory() {
        return recordingDirectory;
    }

    public RecordingSpecification mapsToJournalDirectory() {
        recordToCurrentResourceDir = true;
        enforceJournalModeInScope = true;
        return this;
    }
//For Jackson
    public boolean isRecordToCurrentResourceDir() {
        return recordToCurrentResourceDir;
    }

    public boolean isEnforceJournalModeInScope() {
        return enforceJournalModeInScope;
    }
}
