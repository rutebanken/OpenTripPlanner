package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferMapper {
    private static final Logger LOG = LoggerFactory.getLogger(TransferMapper.class);

    public Transfer mapTransfer(ServiceJourneyInterchange interchange,
            OtpTransitBuilder transitBuilder, NetexDao netexDao) {
        Transfer transfer = new Transfer();

        transfer.setTransferType(1);

        if (interchange.isStaySeated() != null) {
            transfer.setStaySeated(interchange.isStaySeated());
        }
        if (interchange.isGuaranteed() != null) {
            transfer.setGuaranteed(interchange.isGuaranteed());
        }

        String fromStopId = netexDao.quayIdByStopPointRef.lookup(interchange.getFromPointRef().getRef());
        String toStopId = netexDao.quayIdByStopPointRef.lookup(interchange.getToPointRef().getRef());

        if (fromStopId == null || toStopId == null) {
            LOG.warn("Stops not found for transfer " + interchange.getId());
            return null;
        }

        AgencyAndId fromStopAgencyAndId = AgencyAndIdFactory.createAgencyAndId(fromStopId);
        AgencyAndId toStopAgencyAndId = AgencyAndIdFactory.createAgencyAndId(toStopId);

        Stop fromStop = transitBuilder.getStops().containsKey(fromStopAgencyAndId) ?  transitBuilder.getStops().get(fromStopAgencyAndId) : null;
        Stop toStop = transitBuilder.getStops().containsKey(toStopAgencyAndId) ?  transitBuilder.getStops().get(toStopAgencyAndId) : null;

        if (fromStop != null) {
            transfer.setFromStop(fromStop);
        }
        if (toStop != null) {
            transfer.setToStop(toStop);
        }

        Trip fromTrip = transitBuilder.getTrips().get(AgencyAndIdFactory
                .createAgencyAndId(interchange.getFromJourneyRef().getRef()));
        Trip toTrip = transitBuilder.getTrips()
                .get(AgencyAndIdFactory.createAgencyAndId(interchange.getToJourneyRef().getRef()));

        if (fromTrip != null) {
            transfer.setFromTrip(transitBuilder.getTrips().get(AgencyAndIdFactory
                    .createAgencyAndId(interchange.getFromJourneyRef().getRef())));
        }
        if (toTrip != null) {
            transfer.setToTrip(transitBuilder.getTrips()
                    .get(AgencyAndIdFactory.createAgencyAndId(interchange.getToJourneyRef().getRef())));
        }

        if (transfer.getFromTrip() == null || transfer.getToTrip() == null) {
            LOG.warn("Trips not found for transfer " + interchange.getId());
            return null;
        }

        if (transfer.getFromStop() == null || transfer.getToStop() == null) {
            LOG.warn("Stops not found for transfer " + interchange.getId());
            return null;
        }

        transfer.setFromRoute(transfer.getFromTrip().getRoute());
        transfer.setToRoute(transfer.getToTrip().getRoute());

        return transfer;
    }
}
