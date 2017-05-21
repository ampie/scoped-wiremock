package com.github.ampie.wiremock;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "RecordedResponse",
        propOrder = {"status"}
)
public class RecordedResponse extends RecordedMessage{
    @XmlElement(
            nillable = false
    )
    private int status=0;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
