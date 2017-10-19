package org.opentripplanner.netex.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoticeAssignmentMapper {
    private static final Logger LOG = LoggerFactory.getLogger(AgencyMapper.class);

    public org.onebusaway2.gtfs.model.NoticeAssignment mapNoticeAssignment(org.rutebanken.netex.model.NoticeAssignment netexNoticeAssignment){
        org.onebusaway2.gtfs.model.NoticeAssignment otpNoticeAssignment = new org.onebusaway2.gtfs.model.NoticeAssignment();

        otpNoticeAssignment.setId(AgencyAndIdFactory.getAgencyAndId(netexNoticeAssignment.getId()));
        otpNoticeAssignment.setNoticeId(AgencyAndIdFactory.getAgencyAndId(netexNoticeAssignment.getNoticeRef().getRef()));
        otpNoticeAssignment.setElementId(AgencyAndIdFactory.getAgencyAndId(netexNoticeAssignment.getNoticedObjectRef().getRef()));

        return otpNoticeAssignment;
    }
}