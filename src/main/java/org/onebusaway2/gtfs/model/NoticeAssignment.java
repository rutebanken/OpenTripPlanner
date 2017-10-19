package org.onebusaway2.gtfs.model;

import java.io.Serializable;

public class NoticeAssignment implements Serializable{
    public AgencyAndId getId() {
        return Id;
    }

    public void setId(AgencyAndId id) {
        Id = id;
    }

    private AgencyAndId Id;

    public AgencyAndId getNoticeId() {
        return NoticeId;
    }

    public void setNoticeId(AgencyAndId noticeId) {
        NoticeId = noticeId;
    }

    private AgencyAndId NoticeId;

    public AgencyAndId getElementId() {
        return ElementId;
    }

    public void setElementId(AgencyAndId elementId) {
        ElementId = elementId;
    }

    private AgencyAndId ElementId;
}