package org.opentripplanner.netex.mapping;

import org.onebusaway2.gtfs.model.AgencyAndId;
import org.onebusaway2.gtfs.model.Stop;
import org.onebusaway2.gtfs.model.Transfer;
import org.onebusaway2.gtfs.services.GtfsDao;
import org.opentripplanner.graph_builder.model.NetexDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.rutebanken.netex.model.*;

public class TransferMapper {
    private static final Logger LOG = LoggerFactory.getLogger(AgencyMapper.class);

    public Transfer mapTransfer(ServiceJourneyInterchange interchange, GtfsDao gtfsDao, NetexDao netexDao){
        Transfer transfer = new Transfer();

        transfer.setTransferType(1);

        String fromStopId = netexDao.getStopPointQuayMap().get(interchange.getFromPointRef().getRef());
        String toStopId = netexDao.getStopPointQuayMap().get(interchange.getToPointRef().getRef());

        transfer.setFromStop(gtfsDao.getStopForId(AgencyAndIdFactory
                .getAgencyAndId(fromStopId)));
        transfer.setToStop(gtfsDao.getStopForId(AgencyAndIdFactory
                .getAgencyAndId(toStopId)));

        transfer.setFromTrip(gtfsDao.getTripsById().get(AgencyAndIdFactory
                .getAgencyAndId(interchange.getFromJourneyRef().getRef())));
        transfer.setToTrip(gtfsDao.getTripsById().get(AgencyAndIdFactory
                .getAgencyAndId(interchange.getToJourneyRef().getRef())));

        if (transfer.getFromTrip() == null || transfer.getToTrip() == null) {
            LOG.warn("Trips not found for transfer " +  interchange.getId());
        }

        if (transfer.getFromTrip() == null || transfer.getToTrip() == null || transfer.getToStop() == null || transfer.getFromTrip() == null) {
            return null;
        }

        transfer.setFromRoute(transfer.getFromTrip().getRoute());
        transfer.setToRoute(transfer.getToTrip().getRoute());

        return transfer;
    }
}
