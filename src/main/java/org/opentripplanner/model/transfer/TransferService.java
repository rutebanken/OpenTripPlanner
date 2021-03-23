package org.opentripplanner.model.transfer;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents all transfer information in the graph. Transfers are grouped by
 * stop-to-stop pairs.
 */
public class TransferService implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(TransferService.class);

  /** Table which contains transfers between two stops */
  protected Map<P2<TripTransferPoint>, Transfer> trip2tripTransfers;

  /** Table which contains transfers between two stops */
  protected Map<T2<TripTransferPoint, Stop>, Transfer> trip2StopTransfers;

  /** Table which contains transfers between two stops */
  protected Map<T2<Stop, TripTransferPoint>, Transfer> stop2TripTransfers;

  /** Table which contains transfers between two stops */
  protected Map<P2<Stop>, Transfer> stop2StopTransfers;

  public TransferService() {
    this.trip2tripTransfers = new HashMap<>();
    this.trip2StopTransfers = new HashMap<>();
    this.stop2TripTransfers = new HashMap<>();
    this.stop2StopTransfers = new HashMap<>();
  }

  public void addAll(Collection<Transfer> transfers) {
    for (Transfer transfer : transfers) {
      add(transfer);
    }
  }

  public void add(Transfer transfer) {
    TransferPoint from = transfer.getFrom();
    TransferPoint to = transfer.getTo();

    if (from instanceof TripTransferPoint) {
      var fromTrip = (TripTransferPoint) from;
      if (to instanceof TripTransferPoint) {
        var key = new P2<>(fromTrip, (TripTransferPoint)to);
        if(doAddTransferBasedOnSpecificityRanking(transfer, trip2tripTransfers.get(key))) {
          trip2tripTransfers.put(key, transfer);
        }
      }
      else {
        var key = new T2<>(fromTrip, to.getStop());
        if(doAddTransferBasedOnSpecificityRanking(transfer, trip2StopTransfers.get(key))) {
          trip2StopTransfers.put(key, transfer);
        }
      }
    }
    else if (to instanceof TripTransferPoint) {
      var key = new T2<>(from.getStop(), (TripTransferPoint)to);
      if(doAddTransferBasedOnSpecificityRanking(transfer, stop2TripTransfers.get(key))) {
        stop2TripTransfers.put(key, transfer);
      }
    }
    else {
      var key = new P2<>(from.getStop(), to.getStop());
      if(doAddTransferBasedOnSpecificityRanking(transfer, stop2StopTransfers.get(key))) {
        stop2StopTransfers.put(key, transfer);
      }
    }
  }

  public Transfer findTransfer(
      Stop fromStop,
      Stop toStop,
      Trip fromTrip,
      Trip toTrip,
      int fromStopPosition,
      int toStopPosition
  ) {
    var fromTripKey = new TripTransferPoint(fromTrip, fromStopPosition);
    var toTripKey = new TripTransferPoint(toTrip, toStopPosition);
    Transfer result;

    // Check the highest specificity ranked transfers first (trip-2-trip)
    result = trip2tripTransfers.get(new P2<>(fromTripKey, toTripKey));
    if (result != null) { return result; }

    // Then check the next specificity ranked transfers (trip-2-stop and stop-2-trip)
    result = trip2StopTransfers.get(new T2<>(fromTripKey, toStop));
    if (result != null) { return result; }

    // Then check the next specificity ranked transfers (trip-2-stop and stop-2-trip)
    result = stop2TripTransfers.get(new T2<>(fromStop, toTripKey));
    if (result != null) { return result; }

    // If no specificity ranked transfers found return stop-2-stop transfers (lowest ranking)
    return stop2StopTransfers.get(new P2<>(fromStop, toStop));
  }

  private boolean doAddTransferBasedOnSpecificityRanking(
      Transfer newTransfer,
      Transfer existingTransfer
  ) {
    if(existingTransfer == null) { return true; }

    if(existingTransfer.getSpecificityRanking() < newTransfer.getSpecificityRanking()) {
      return true;
    }
    if(existingTransfer.getSpecificityRanking() > newTransfer.getSpecificityRanking()) {
      return false;
    }

    LOG.error(
        "To colliding transfers A abd B with the same specificity-ranking is imported, B is "
            + "dropped. A={}, B={}", existingTransfer, newTransfer
    );
    return false;
  }
}
