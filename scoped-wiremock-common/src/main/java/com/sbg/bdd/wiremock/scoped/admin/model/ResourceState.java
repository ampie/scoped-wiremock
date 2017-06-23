package com.sbg.bdd.wiremock.scoped.admin.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.codec.binary.Base64;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "ResourceState",
        propOrder = {"resourceRoot", "path", "data", "type"}
)
public class ResourceState {
    @XmlElement(
            nillable = false
    )
    private String resourceRoot;
    @XmlElement(
            nillable = false
    )
    private String path;
    @XmlElement(
            nillable = true
    )
    private byte[] data;
    @XmlElement(
            nillable = true
    )
    private ResourceType type;

    public ResourceState(String resourceRoot, String path) {
        this.resourceRoot = resourceRoot;
        this.path = path;
    }

    public ResourceState() {

    }

    public String getPath() {
        return path;
    }

    public String getResourceRoot() {
        return resourceRoot;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    @JsonIgnore
    public String getName() {
        return getPath().substring(getPath().lastIndexOf("/") + 1);
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
