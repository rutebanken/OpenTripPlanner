package org.onebusaway2.gtfs.model;

import java.io.Serializable;

public class Notice implements Serializable{
    public AgencyAndId getId() {
        return Id;
    }

    public void setId(AgencyAndId id) {
        Id = id;
    }

    public String getText() {
        return Text;
    }

    public void setText(String text) {
        Text = text;
    }

    public String getPublicCode() {
        return PublicCode;
    }

    public void setPublicCode(String publicCode) {
        PublicCode = publicCode;
    }

    private AgencyAndId Id;
    private String Text;
    private String PublicCode;
}