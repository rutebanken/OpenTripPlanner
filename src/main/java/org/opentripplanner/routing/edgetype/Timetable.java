/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype;

import com.beust.jcommander.internal.Lists;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StopTransfer;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.*;

import javax.xml.datatype.Duration;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;
import static org.opentripplanner.model.StopPattern.PICKDROP_SCHEDULED;


/**
 * Timetables provide most of the TripPattern functionality. Each TripPattern may possess more than
 * one Timetable when stop time updates are being applied: one for the scheduled stop times, one for
 * each snapshot of updated stop times, another for a working buffer of updated stop times, etc.
 */
public class Timetable implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(Timetable.class);
    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    /**
     * A circular reference between TripPatterns and their scheduled (non-updated) timetables.
     */
    public final TripPattern pattern;

    /**
     * Contains one TripTimes object for each scheduled trip (even cancelled ones) and possibly
     * additional TripTimes objects for unscheduled trips. Frequency entries are stored separately.
     */
    public final List<TripTimes> tripTimes = new CopyOnWriteArrayList();

    /**
     * Contains one FrequencyEntry object for each block of frequency-based trips.
     */
    public final List<FrequencyEntry> frequencyEntries = Lists.newArrayList();

    /**
     * The ServiceDate for which this (updated) timetable is valid. If null, then it is valid for all dates.
     */
    public final ServiceDate serviceDate;

    /**
     * For each hop, the best running time. This serves to provide lower bounds on traversal time.
     */
    private transient int minRunningTimes[];

    /**
     * For each stop, the best dwell time. This serves to provide lower bounds on traversal time.
     */
    private transient int minDwellTimes[];

    /**
     * Helps determine whether a particular pattern is worth searching for departures at a given time.
     */
    private transient int minTime, maxTime;

    /**
     * Construct an empty Timetable.
     */
    public Timetable(TripPattern pattern) {
        this.pattern = pattern;
        this.serviceDate = null;
    }

    /**
     * Construct an empty Timetable with a specified serviceDate.
     */
    public Timetable(TripPattern pattern, ServiceDate serviceDate) {
        this.pattern = pattern;
        this.serviceDate = serviceDate;
    }

    /**
     * Copy constructor: create an un-indexed Timetable with the same TripTimes as the specified timetable.
     */
    Timetable(Timetable tt, ServiceDate serviceDate) {
        tripTimes.addAll(tt.tripTimes);
        this.serviceDate = serviceDate;
        this.pattern = tt.pattern;
    }

    /**
     * Before performing the relatively expensive iteration over all the trips in this pattern, check whether it's even
     * possible to board any of them given the time at which we are searching, and whether it's possible that any of
     * them could improve on the best known time. This is only an optimization, but a significant one. When we search
     * for departures, we look at three separate days: yesterday, today, and tomorrow. Many patterns do not have
     * service at all hours of the day or past midnight. This optimization can cut the search time for each pattern
     * by 66 to 100 percent.
     *
     * @param bestWait -1 means there is not yet any best known time.
     */
    public boolean temporallyViable(ServiceDay sd, long searchTime, int bestWait, boolean boarding) {
        // TODO: From flex-merge - check this
        if (this.pattern.services == null)
            return true;
        // Check whether any services are running at all on this pattern.
        if (!sd.anyServiceRunning(this.pattern.services)) return false;
        // Make the search time relative to the given service day.
        searchTime = sd.secondsSinceMidnight(searchTime);
        // Check whether any trip can be boarded at all, given the search time
        if (boarding ? (searchTime > this.maxTime) : (searchTime < this.minTime)) return false;
        // Check whether any trip can improve on the best time yet found
        if (bestWait >= 0) {
            long bestTime = boarding ? (searchTime + bestWait) : (searchTime - bestWait);
            if (boarding ? (bestTime < this.minTime) : (bestTime > this.maxTime)) return false;
        }
        return true;
    }

    /**
     * Get the next (previous) trip that departs (arrives) from the specified stop at or after
     * (before) the specified time.
     *
     * @return the TripTimes object representing the (possibly updated) best trip, or null if no
     * trip matches both the time and other criteria.
     */
    public TripTimes getNextTrip(State s0, ServiceDay serviceDay, int stopIndex, boolean boarding, double flexOffsetScale, int preBoardDirectTime, int postAlightDirectTime) {
        /* Search at the state's time, but relative to midnight on the given service day. */
        int time = getSecondsSinceStartOfRelevantDate(s0.getTimeSeconds(), serviceDay);
        // NOTE the time is sometimes negative here. That is fine, we search for the first trip of the day.
        // Adjust for possible boarding time TODO: This should be included in the trip and based on GTFS
        if (boarding) {
            time += s0.getOptions().getBoardTime(this.pattern.mode);
        } else {
            time -= s0.getOptions().getAlightTime(this.pattern.mode);
        }
        TripTimes bestTrip = null;
        Stop currentStop = pattern.getStop(stopIndex);
        // Linear search through the timetable looking for the best departure.
        // We no longer use a binary search on Timetables because:
        // 1. we allow combining trips from different service IDs on the same tripPattern.
        // 2. We mix frequency-based and one-off TripTimes together on tripPatterns.
        // 3. Stoptimes may change with realtime updates, and we cannot count on them being sorted.
        //    The complexity of keeping sorted indexes up to date does not appear to be worth the
        //    apparently minor speed improvement.
        int bestTime = boarding ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        // Hoping JVM JIT will distribute the loop over the if clauses as needed.
        // We could invert this and skip some service days based on schedule overlap as in RRRR.
        for (TripTimes tt : tripTimes) {
            if (tt.isCanceled()) continue;
            if ((tt.getNumStops() <= stopIndex)) continue;
            if (!serviceDay.serviceRunning(tt.serviceCode)) continue; // TODO merge into call on next line
            if (!tt.tripAcceptable(s0, stopIndex)) continue;
            if (s0.getOptions().tripIsBanned(tt.trip)) continue;
            int adjustedTime = adjustTimeForTransfer(s0, currentStop, tt.trip, boarding, serviceDay, time);
            if (adjustedTime == -1) continue;
            if (boarding) {
                int adjustment = 0;
                if (stopIndex + 1 < tt.getNumStops() && flexOffsetScale != 0.0) {
                    adjustment = (int) Math.round(flexOffsetScale*tt.getRunningTime(stopIndex));
                }
                int vehicleTime = (preBoardDirectTime == 0) ? 0 : tt.getDemandResponseMaxTime(preBoardDirectTime);
                int depTime = tt.getDepartureTime(stopIndex) + adjustment - vehicleTime;
                if (depTime < 0)
                    continue; // negative values were previously used for canceled trips/passed stops/skipped stops, but
                // now its not sure if this check should be still in place because there is a boolean field
                // for canceled trips
                if (depTime >= adjustedTime && depTime < bestTime) {
                    bestTrip = tt;
                    bestTime = depTime;
                }
            } else {
                int adjustment = 0;
                if (stopIndex - 1 >= 0 && flexOffsetScale != 0.0) {
                    adjustment = (int) Math.round(flexOffsetScale*tt.getRunningTime(stopIndex - 1));
                }
                int vehicleTime = (postAlightDirectTime == 0) ? 0 : tt.getDemandResponseMaxTime(postAlightDirectTime);
                int arvTime = tt.getArrivalTime(stopIndex) + adjustment + vehicleTime;
                if (arvTime < 0) continue;
                if (arvTime <= adjustedTime && arvTime > bestTime) {
                    bestTrip = tt;
                    bestTime = arvTime;
                }
            }
        }
        // ACK all logic is identical to above.
        // A sign that FrequencyEntries and TripTimes need a common interface.
        FrequencyEntry bestFreq = null;
        for (FrequencyEntry freq : frequencyEntries) {
            TripTimes tt = freq.tripTimes;
            if (tt.isCanceled()) continue;
            if (!serviceDay.serviceRunning(tt.serviceCode)) continue; // TODO merge into call on next line
            if (!tt.tripAcceptable(s0, stopIndex)) continue;
            int adjustedTime = adjustTimeForTransfer(s0, currentStop, tt.trip, boarding, serviceDay, time);
            if (adjustedTime == -1) continue;
            LOG.debug("  running freq {}", freq);
            if (boarding) {
                int depTime = freq.nextDepartureTime(stopIndex, adjustedTime); // min transfer time included in search
                if (depTime < 0) continue;
                if (depTime >= adjustedTime && depTime < bestTime) {
                    bestFreq = freq;
                    bestTime = depTime;
                }
            } else {
                int arvTime = freq.prevArrivalTime(stopIndex, adjustedTime); // min transfer time included in search
                if (arvTime < 0) continue;
                if (arvTime <= adjustedTime && arvTime > bestTime) {
                    bestFreq = freq;
                    bestTime = arvTime;
                }
            }
        }
        if (bestFreq != null) {
            // A FrequencyEntry beat all the TripTimes.
            // Materialize that FrequencyEntry entry at the given time.
            bestTrip = bestFreq.tripTimes.timeShift(stopIndex, bestTime, boarding);
        }
        return bestTrip;
    }

    public TripTimes getNextTrip(State s0, ServiceDay serviceDay, int stopIndex, boolean boarding) {
        return getNextTrip(s0, serviceDay, stopIndex, boarding, 0, 0, 0);
    }

    // could integrate with getNextTrip
    public TripTimes getNextCallNRideTrip(State s0, ServiceDay serviceDay, int stopIndex, boolean boarding, int directTime) {
        /* Search at the state's time, but relative to midnight on the given service day. */
        int time = serviceDay.secondsSinceMidnight(s0.getTimeSeconds());
        // NOTE the time is sometimes negative here. That is fine, we search for the first trip of the day.
        // Adjust for possible boarding time TODO: This should be included in the trip and based on GTFS
        if (boarding) {
            time += s0.getOptions().getBoardTime(this.pattern.mode);
        } else {
            time -= s0.getOptions().getAlightTime(this.pattern.mode);
        }
        TripTimes bestTrip = null;
        Stop currentStop = pattern.getStop(stopIndex);
        long bestTime = boarding ? Long.MAX_VALUE : Long.MIN_VALUE;
        boolean useClockTime = !s0.getOptions().ignoreDrtAdvanceBookMin;
        long clockTime = s0.getOptions().clockTimeSec;
        for (TripTimes tt : tripTimes) {
            if (tt.isCanceled()) continue;
            if ( ! serviceDay.serviceRunning(tt.serviceCode)) continue; // TODO merge into call on next line
            if ( ! tt.tripAcceptable(s0, stopIndex)) continue;
            int adjustedTime = adjustTimeForTransfer(s0, currentStop, tt.trip, boarding, serviceDay, time);
            if (adjustedTime == -1) continue;
            if (boarding) {
                long depTime = tt.getCallAndRideBoardTime(stopIndex, adjustedTime, serviceDay, useClockTime, clockTime);
                if (depTime >= adjustedTime && depTime < bestTime && inBounds(depTime)) {
                    bestTrip = tt;
                    bestTime = depTime;
                }
            } else {
                long arvTime = tt.getCallAndRideAlightTime(stopIndex, adjustedTime, directTime, serviceDay, useClockTime, clockTime);
                if (arvTime < 0) continue;
                if (arvTime <= adjustedTime && arvTime > bestTime && inBounds(arvTime)) {
                    bestTrip = tt;
                    bestTime = arvTime;
                }
            }
        }

        return bestTrip;
    }

    private boolean inBounds(long time) {
        return time >= minTime && time <= maxTime;
    }

    /**
     * Convert seconds since epoch to seconds since midnight for the relevant service date.
     * <p>
     * if this timetable is only valid for a single date, this date will be used as baseline. If not, the provided
     * serviceDay will be used as baseline.
     * <p>
     * Always using the provided serviceDay caused timetables with realtime updates valid for a single day to be matched with the wrong day.
     */
    private int getSecondsSinceStartOfRelevantDate(long secondsSinceEpoch, ServiceDay serviceDay) {
        if (serviceDate != null) {
            return (int) (secondsSinceEpoch - (serviceDate.getAsDate().getTime() / 1000));
        }
        return serviceDay.secondsSinceMidnight(secondsSinceEpoch);
    }

    /**
     * Check transfer table rules. Given the last alight time from the State,
     * return the boarding time t0 adjusted for this particular trip's minimum transfer time,
     * or -1 if boarding this trip is not allowed.
     * FIXME adjustedTime can legitimately be -1! But negative times might as well be zero.
     */
    private int adjustTimeForTransfer(State state, Stop currentStop, Trip trip, boolean boarding, ServiceDay serviceDay, int t0) {
        if (!state.isEverBoarded()) {
            // This is the first boarding not a transfer.
            return t0;
        }
        TransferTable transferTable = state.getOptions().getRoutingContext().transferTable;
        int transferTime = transferTable.getTransferTime(state.getPreviousStop(), currentStop, state.getPreviousTrip(), trip, boarding, state);
        // Check whether back edge is TimedTransferEdge
        if (state.getBackEdge() instanceof TimedTransferEdge) {
            // Transfer must be of type TIMED_TRANSFER
            if (transferTime != StopTransfer.TIMED_TRANSFER) {
                return -1;
            }
        }
        if (transferTime == StopTransfer.UNKNOWN_TRANSFER) {
            return t0; // no special rules, just board
        }
        if (transferTime == StopTransfer.FORBIDDEN_TRANSFER) {
            // This transfer is forbidden
            return -1;
        }
        // There is a minimum transfer time to make this transfer. Ensure that it is respected.
        int minTime = getSecondsSinceStartOfRelevantDate(state.getLastAlightedTimeSeconds(), serviceDay);
        if (boarding) {
            minTime += transferTime;
            if (minTime > t0) return minTime;
        } else {
            minTime -= transferTime;
            if (minTime < t0) return minTime;
        }
        return t0;
    }

    /**
     * Finish off a Timetable once all TripTimes have been added to it. This involves caching
     * lower bounds on the running times and dwell times at each stop, and may perform other
     * actions to compact the data structure such as trimming and deduplicating arrays.
     */
    public void finish() {
        int nStops = pattern.stopPattern.size;
        int nHops = nStops - 1;
        /* Find lower bounds on dwell and running times at each stop. */
        minDwellTimes = new int[nHops];
        minRunningTimes = new int[nHops];
        Arrays.fill(minDwellTimes, Integer.MAX_VALUE);
        Arrays.fill(minRunningTimes, Integer.MAX_VALUE);
        // Concatenate raw TripTimes and those referenced from FrequencyEntries
        List<TripTimes> allTripTimes = Lists.newArrayList(tripTimes);
        for (FrequencyEntry freq : frequencyEntries) allTripTimes.add(freq.tripTimes);

        minTime = Integer.MAX_VALUE;
        maxTime = Integer.MIN_VALUE;

        for (TripTimes tt : allTripTimes) {
            if (tt.getNumStops() == nStops) {
                for (int h = 0; h < nHops; ++h) {
                    int dt = tt.getDwellTime(h);
                    if (minDwellTimes[h] > dt) {
                        minDwellTimes[h] = dt;
                    }
                    int rt = tt.getRunningTime(h);
                    if (minRunningTimes[h] > rt) {
                        minRunningTimes[h] = rt;
                    }
                }
                minTime = Math.min(minTime, tt.getDepartureTime(0));
                maxTime = Math.max(maxTime, tt.getArrivalTime(nStops - 1));
            }
        }

        for (TripTimes tt : tripTimes) {
            if (tt.getNumStops() == nStops) {
                minTime = Math.min(minTime, tt.getDepartureTime(0));
                maxTime = Math.max(maxTime, tt.getArrivalTime(nStops - 1));
            }
        }
        // Slightly repetitive code.
        // Again it seems reasonable to have a shared interface between FrequencyEntries and normal TripTimes.
        for (FrequencyEntry freq : frequencyEntries) {
            minTime = Math.min(minTime, freq.getMinDeparture());
            maxTime = Math.max(maxTime, freq.getMaxArrival());
        }
    }

    /**
     * @return the index of TripTimes for this trip ID in this particular Timetable
     */
    public int getTripIndex(AgencyAndId tripId) {
        int ret = 0;
        for (TripTimes tt : tripTimes) {
            // could replace linear search with indexing in stoptime updater, but not necessary
            // at this point since the updater thread is far from pegged.
            if (tt.trip.getId().equals(tripId)) return ret;
            ret += 1;
        }
        return -1;
    }

    /**
     * @return the matching Trip in this particular Timetable
     */
    public Trip getTrip(AgencyAndId tripId) {
        for (TripTimes tt : tripTimes) {
            if (tt.trip.getId().equals(tripId)) {
                return tt.trip;
            }
        }
        return null;
    }

    /**
     * @return the index of TripTimes for this trip ID in this particular Timetable, ignoring AgencyIds.
     */
    public int getTripIndex(String tripId) {
        int ret = 0;
        for (TripTimes tt : tripTimes) {
            if (tt.trip.getId().getId().equals(tripId)) return ret;
            ret += 1;
        }
        return -1;
    }

    public TripTimes getTripTimes(int tripIndex) {
        return tripTimes.get(tripIndex);
    }

    public TripTimes getTripTimes(Trip trip) {
        for (TripTimes tt : tripTimes) {
            if (tt.trip == trip) return tt;
        }
        return null;
    }

    /**
     * Set new trip times for trip given a trip index
     *
     * @param tripIndex trip index of trip
     * @param tt        new trip times for trip
     * @return old trip times of trip
     */
    public TripTimes setTripTimes(int tripIndex, TripTimes tt) {
        return tripTimes.set(tripIndex, tt);
    }

    /**
     * Apply the TripUpdate to the appropriate TripTimes from this Timetable. The existing TripTimes
     * must not be modified directly because they may be shared with the underlying
     * scheduledTimetable, or other updated Timetables. The {@link TimetableSnapshot} performs the
     * protective copying of this Timetable. It is not done in this update method to avoid
     * repeatedly cloning the same Timetable when several updates are applied to it at once. We
     * assume here that all trips in a timetable are from the same feed, which should always be the
     * case.
     *
     * @param tripUpdate        GTFS-RT trip update
     * @param timeZone          time zone of trip update
     * @param updateServiceDate service date of trip update
     * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
     * with the id specified in the trip descriptor of the TripUpdate; null if something
     * went wrong
     */
    public TripTimes createUpdatedTripTimes(TripUpdate tripUpdate, TimeZone timeZone, ServiceDate updateServiceDate) {
        if (tripUpdate == null) {
            LOG.error("A null TripUpdate pointer was passed to the Timetable class update method.");
            return null;
        }

        // Though all timetables have the same trip ordering, some may have extra trips due to
        // the dynamic addition of unscheduled trips.
        // However, we want to apply trip updates on top of *scheduled* times
        if (!tripUpdate.hasTrip()) {
            LOG.error("TripUpdate object has no TripDescriptor field.");
            return null;
        }

        TripDescriptor tripDescriptor = tripUpdate.getTrip();
        if (!tripDescriptor.hasTripId()) {
            LOG.error("TripDescriptor object has no TripId field");
            return null;
        }
        String tripId = tripDescriptor.getTripId();
        int tripIndex = getTripIndex(tripId);
        if (tripIndex == -1) {
            LOG.info("tripId {} not found in pattern.", tripId);
            return null;
        } else {
            LOG.trace("tripId {} found at index {} in timetable.", tripId, tripIndex);
        }

        TripTimes newTimes = new TripTimes(getTripTimes(tripIndex));

        if (tripDescriptor.hasScheduleRelationship() && tripDescriptor.getScheduleRelationship()
                                                                == TripDescriptor.ScheduleRelationship.CANCELED) {
            newTimes.cancel();
        } else {
            // The GTFS-RT reference specifies that StopTimeUpdates are sorted by stop_sequence.
            Iterator<StopTimeUpdate> updates = tripUpdate.getStopTimeUpdateList().iterator();
            if (!updates.hasNext()) {
                LOG.warn("Won't apply zero-length trip update to trip {}.", tripId);
                return null;
            }
            StopTimeUpdate update = updates.next();

            int numStops = newTimes.getNumStops();
            Integer delay = null;

            for (int i = 0; i < numStops; i++) {
                boolean match = false;
                if (update != null) {
                    if (update.hasStopSequence()) {
                        match = update.getStopSequence() == newTimes.getStopSequence(i);
                    } else if (update.hasStopId()) {
                        match = pattern.getStop(i).getId().getId().equals(update.getStopId());
                    }
                }

                if (match) {
                    StopTimeUpdate.ScheduleRelationship scheduleRelationship =
                            update.hasScheduleRelationship() ? update.getScheduleRelationship()
                                    : StopTimeUpdate.ScheduleRelationship.SCHEDULED;
                    if (scheduleRelationship == StopTimeUpdate.ScheduleRelationship.SKIPPED) {
                        LOG.warn("Partially canceled trips are unsupported by this method." +
                                         " Skipping TripUpdate.");
                        return null;
                    } else if (scheduleRelationship ==
                                       StopTimeUpdate.ScheduleRelationship.NO_DATA) {
                        newTimes.updateArrivalDelay(i, 0);
                        newTimes.updateDepartureDelay(i, 0);
                        delay = 0;
                    } else {
                        long today = updateServiceDate.getAsDate(timeZone).getTime() / 1000;

                        if (update.hasArrival() && (update.getArrival().hasTime() || update.getArrival().hasDelay())) {
                            StopTimeEvent arrival = update.getArrival();
                            if (arrival.hasDelay()) {
                                delay = arrival.getDelay();
                                if (arrival.hasTime()) {
                                    newTimes.updateArrivalTime(i,
                                            (int) (arrival.getTime() - today));
                                } else {
                                    newTimes.updateArrivalDelay(i, delay);
                                }
                            } else if (arrival.hasTime()) {
                                newTimes.updateArrivalTime(i,
                                        (int) (arrival.getTime() - today));
                                delay = newTimes.getArrivalDelay(i);
                            } else {
                                LOG.error("Arrival time at index {} is erroneous.", i);
                                return null;
                            }
                        } else {
                            if (delay == null) {
                                newTimes.updateArrivalTime(i, TripTimes.UNAVAILABLE);
                            } else {
                                newTimes.updateArrivalDelay(i, delay);
                            }
                        }

                        if (update.hasDeparture() && (update.getDeparture().hasTime() || update.getDeparture().hasDelay())) {
                            StopTimeEvent departure = update.getDeparture();
                            if (departure.hasDelay()) {
                                delay = departure.getDelay();
                                if (departure.hasTime()) {
                                    newTimes.updateDepartureTime(i,
                                            (int) (departure.getTime() - today));
                                } else {
                                    newTimes.updateDepartureDelay(i, delay);
                                }
                            } else if (departure.hasTime()) {
                                newTimes.updateDepartureTime(i,
                                        (int) (departure.getTime() - today));
                                delay = newTimes.getDepartureDelay(i);
                            } else {
                                LOG.error("Departure time at index {} is erroneous.", i);
                                return null;
                            }
                        } else {
                            if (delay == null) {
                                newTimes.updateDepartureTime(i, TripTimes.UNAVAILABLE);
                            } else {
                                newTimes.updateDepartureDelay(i, delay);
                            }
                        }
                    }

                    if (updates.hasNext()) {
                        update = updates.next();
                    } else {
                        update = null;
                    }
                } else {
                    if (delay == null) {
                        newTimes.updateArrivalTime(i, TripTimes.UNAVAILABLE);
                        newTimes.updateDepartureTime(i, TripTimes.UNAVAILABLE);
                    } else {
                        newTimes.updateArrivalDelay(i, delay);
                        newTimes.updateDepartureDelay(i, delay);
                    }
                }
            }
            if (update != null) {
                LOG.error("Part of a TripUpdate object could not be applied successfully to trip {}.", tripId);
                return null;
            }
        }
        if (!newTimes.timesIncreasing()) {
            LOG.error("TripTimes are non-increasing after applying GTFS-RT delay propagation to trip {}.", tripId);
            return null;
        }

        LOG.debug("A valid TripUpdate object was applied to trip {} using the Timetable class update method.", tripId);
        return newTimes;
    }

    /**
     * Apply the TripUpdate to the appropriate TripTimes from this Timetable. The existing TripTimes
     * must not be modified directly because they may be shared with the underlying
     * scheduledTimetable, or other updated Timetables. The {@link TimetableSnapshot} performs the
     * protective copying of this Timetable. It is not done in this update method to avoid
     * repeatedly cloning the same Timetable when several updates are applied to it at once. We
     * assume here that all trips in a timetable are from the same feed, which should always be the
     * case.
     *
     * @param journey  SIRI-ET EstimatedVehicleJourney
     * @param timeZone time zone of trip update
     * @param tripId
     * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
     * with the id specified in the trip descriptor of the TripUpdate; null if something
     * went wrong
     */
    public TripTimes createUpdatedTripTimes(final Graph graph, EstimatedVehicleJourney journey, TimeZone timeZone, AgencyAndId tripId) {
        if (journey == null) {
            return null;
        }

        int tripIndex = getTripIndex(tripId);
        if (tripIndex == -1) {
            LOG.debug("tripId {} not found in pattern.", tripId);
            return null;
        }

        final TripTimes existingTripTimes = getTripTimes(tripIndex);
        TripTimes oldTimes = new TripTimes(existingTripTimes);

        if (journey.isCancellation() != null && journey.isCancellation()) {
            oldTimes.cancel();
            return oldTimes;
        }

        EstimatedVehicleJourney.EstimatedCalls journeyEstimatedCalls = journey.getEstimatedCalls();
        EstimatedVehicleJourney.RecordedCalls journeyRecordedCalls = journey.getRecordedCalls();

        if (journeyEstimatedCalls == null) {
            return null;
        }

        List<EstimatedCall> estimatedCalls = journeyEstimatedCalls.getEstimatedCalls();
        List<RecordedCall> recordedCalls;
        if (journeyRecordedCalls != null) {
            recordedCalls = journeyRecordedCalls.getRecordedCalls();
        } else {
            recordedCalls = new ArrayList<>();
        }

        boolean stopPatternChanged = false;

        Stop[] modifiedStops = pattern.stopPattern.stops;

        Trip trip = getTrip(tripId);

        List<StopTime> modifiedStopTimes = createModifiedStopTimes(oldTimes, journey, trip, graph.index);
        if (modifiedStopTimes == null) {
            return null;
        }
        TripTimes newTimes = new TripTimes(trip, modifiedStopTimes, graph.deduplicator);

        //Populate missing data from existing TripTimes
        newTimes.serviceCode = oldTimes.serviceCode;

        int callCounter = 0;
        ZonedDateTime departureDate = null;
        Set<Object> alreadyVisited = new HashSet<>();

        boolean isJourneyPredictionInaccurate =  (journey.isPredictionInaccurate() != null && journey.isPredictionInaccurate());

        int departureFromPreviousStop = 0;
        int lastArrivalDelay = 0;
        int lastDepartureDelay = 0;
        for (Stop stop : modifiedStops) {
            boolean foundMatch = false;

            for (RecordedCall recordedCall : recordedCalls) {
                if (alreadyVisited.contains(recordedCall)) {
                    continue;
                }
                //Current stop is being updated
                foundMatch = stop.getId().getId().equals(recordedCall.getStopPointRef().getValue());

                if (!foundMatch && stop.getParentStation() != null) {
                    Stop alternativeStop = graph.index.stopForId.get(new AgencyAndId(stop.getId().getAgencyId(), recordedCall.getStopPointRef().getValue()));
                    if (alternativeStop != null && stop.getParentStation().equals(alternativeStop.getParentStation())) {
                        foundMatch = true;
                        stopPatternChanged = true;
                    }
                }

                if (foundMatch) {
                    if (departureDate == null) {
                        departureDate = recordedCall.getAimedDepartureTime();
                        if (departureDate == null) {
                            departureDate = recordedCall.getAimedArrivalTime();
                        }
                    }

                    //Flag as recorded
                    newTimes.setRecorded(callCounter, true);

                    if (recordedCall.isCancellation() != null) {
                        newTimes.setCancelledStop(callCounter, recordedCall.isCancellation());
                    }

                    newTimes.setDropoffType(callCounter, pattern.stopPattern.dropoffs[callCounter]);
                    newTimes.setPickupType(callCounter, pattern.stopPattern.pickups[callCounter]);

                    int arrivalTime = newTimes.getArrivalTime(callCounter);
                    int realtimeArrivalTime = arrivalTime;
                    if (recordedCall.getActualArrivalTime() != null) {
                        realtimeArrivalTime = calculateSecondsSinceMidnight(departureDate, recordedCall.getActualArrivalTime());
                    } else if (recordedCall.getExpectedArrivalTime() != null) {
                        realtimeArrivalTime = calculateSecondsSinceMidnight(departureDate, recordedCall.getExpectedArrivalTime());
                    } else if (recordedCall.getAimedArrivalTime() != null) {
                        realtimeArrivalTime = calculateSecondsSinceMidnight(departureDate, recordedCall.getAimedArrivalTime());
                    }
                    int arrivalDelay = realtimeArrivalTime - arrivalTime;
                    newTimes.updateArrivalDelay(callCounter, arrivalDelay);
                    lastArrivalDelay = arrivalDelay;

                    int departureTime = newTimes.getDepartureTime(callCounter);
                    int realtimeDepartureTime = departureTime;
                    if (recordedCall.getActualDepartureTime() != null) {
                        realtimeDepartureTime = calculateSecondsSinceMidnight(departureDate, recordedCall.getActualDepartureTime());
                    } else if (recordedCall.getExpectedDepartureTime() != null) {
                        realtimeDepartureTime = calculateSecondsSinceMidnight(departureDate, recordedCall.getExpectedDepartureTime());
                    } else if (recordedCall.getAimedDepartureTime() != null) {
                        realtimeDepartureTime = calculateSecondsSinceMidnight(departureDate, recordedCall.getAimedDepartureTime());
                    }
                    if (realtimeDepartureTime < realtimeArrivalTime) {
                        realtimeDepartureTime = realtimeArrivalTime;
                    }
                    int departureDelay = realtimeDepartureTime - departureTime;

                    newTimes.updateDepartureDelay(callCounter, departureDelay);
                    lastDepartureDelay = departureDelay;
                    departureFromPreviousStop = newTimes.getDepartureTime(callCounter);

                    alreadyVisited.add(recordedCall);
                    break;
                }
            }
            if (!foundMatch) {
                for (EstimatedCall estimatedCall : estimatedCalls) {
                    if (alreadyVisited.contains(estimatedCall)) {
                        continue;
                    }
                    //Current stop is being updated
                    foundMatch = stop.getId().getId().equals(estimatedCall.getStopPointRef().getValue());

                    if (!foundMatch && stop.getParentStation() != null) {
                        Stop alternativeStop = graph.index.stopForId.get(new AgencyAndId(stop.getId().getAgencyId(), estimatedCall.getStopPointRef().getValue()));
                        if (alternativeStop != null && stop.getParentStation().equals(alternativeStop.getParentStation())) {
                            foundMatch = true;
                            stopPatternChanged = true;
                        }
                    }

                    if (foundMatch) {
                        if (departureDate == null) {
                            departureDate = estimatedCall.getAimedDepartureTime();
                            if (departureDate == null) {
                                departureDate = estimatedCall.getAimedArrivalTime();
                            }
                        }

                        if (estimatedCall.isCancellation() != null) {
                            newTimes.setCancelledStop(callCounter, estimatedCall.isCancellation());
                        }

                        boolean isCallPredictionInaccurate = estimatedCall.isPredictionInaccurate() != null && estimatedCall.isPredictionInaccurate();

                        // Set flag for inaccurate prediction if either call OR journey has inaccurate-flag set.
                        newTimes.setPredictionInaccurate(callCounter, (isJourneyPredictionInaccurate | isCallPredictionInaccurate));

                        // Update dropoff-/pickuptype only if status is cancelled
                        CallStatusEnumeration arrivalStatus = estimatedCall.getArrivalStatus();
                        if (arrivalStatus == CallStatusEnumeration.CANCELLED) {
                            newTimes.setDropoffType(callCounter, PICKDROP_NONE);
                        }

                        CallStatusEnumeration departureStatus = estimatedCall.getDepartureStatus();
                        if (departureStatus == CallStatusEnumeration.CANCELLED) {
                            newTimes.setPickupType(callCounter, PICKDROP_NONE);
                        }

                        int arrivalTime = newTimes.getArrivalTime(callCounter);
                        int realtimeArrivalTime = -1;
                        if (estimatedCall.getExpectedArrivalTime() != null) {
                            realtimeArrivalTime = calculateSecondsSinceMidnight(departureDate, estimatedCall.getExpectedArrivalTime());
                        } else if (estimatedCall.getAimedArrivalTime() != null) {
                            realtimeArrivalTime = calculateSecondsSinceMidnight(departureDate, estimatedCall.getAimedArrivalTime());
                        }

                        int departureTime = newTimes.getDepartureTime(callCounter);
                        int realtimeDepartureTime = departureTime;
                        if (estimatedCall.getExpectedDepartureTime() != null) {
                            realtimeDepartureTime = calculateSecondsSinceMidnight(departureDate, estimatedCall.getExpectedDepartureTime());
                        } else if (estimatedCall.getAimedDepartureTime() != null) {
                            realtimeDepartureTime = calculateSecondsSinceMidnight(departureDate, estimatedCall.getAimedDepartureTime());
                        }

                        if (realtimeArrivalTime == -1) {
                            realtimeArrivalTime = realtimeDepartureTime;
                        }
                        if (realtimeDepartureTime < realtimeArrivalTime) {
                            realtimeDepartureTime = realtimeArrivalTime;
                        }

                        int arrivalDelay = realtimeArrivalTime - arrivalTime;
                        newTimes.updateArrivalDelay(callCounter, arrivalDelay);
                        lastArrivalDelay = arrivalDelay;

                        int departureDelay = realtimeDepartureTime - departureTime;
                        newTimes.updateDepartureDelay(callCounter, departureDelay);
                        lastDepartureDelay = departureDelay;

                        departureFromPreviousStop = newTimes.getDepartureTime(callCounter);

                        alreadyVisited.add(estimatedCall);
                        break;
                    }
                }
            }
            if (!foundMatch) {

                if (pattern.stopPattern.pickups[callCounter] == PICKDROP_NONE &&
                            pattern.stopPattern.dropoffs[callCounter] == PICKDROP_NONE) {
                    // When newTimes contains stops without pickup/dropoff - set both arrival/departure to previous stop's departure
                    // This necessary to accommodate the case when delay is reduced/eliminated between to stops with pickup/dropoff, and
                    // multiple non-pickup/dropoff stops are in between.
                    newTimes.updateArrivalTime(callCounter, departureFromPreviousStop);
                    newTimes.updateDepartureTime(callCounter, departureFromPreviousStop);
                } else {

                    int arrivalDelay = lastArrivalDelay;
                    int departureDelay = lastDepartureDelay;

                    if (lastArrivalDelay == 0 && lastDepartureDelay == 0) {
                        //No match has been found yet (i.e. still in RecordedCalls) - keep existing delays
                        arrivalDelay = existingTripTimes.getArrivalDelay(callCounter);
                        departureDelay = existingTripTimes.getDepartureDelay(callCounter);
                    }

                    newTimes.updateArrivalDelay(callCounter, arrivalDelay);
                    newTimes.updateDepartureDelay(callCounter, departureDelay);
                }

                departureFromPreviousStop = newTimes.getDepartureTime(callCounter);
            }
            callCounter++;
        }

        if (stopPatternChanged) {
            // This update modified stopPattern
            newTimes.setRealTimeState(RealTimeState.MODIFIED);
        } else {
            // This is the first update, and StopPattern has not been changed
            newTimes.setRealTimeState(RealTimeState.UPDATED);
        }

        if (journey.isCancellation() != null && journey.isCancellation()) {
            LOG.debug("Trip is cancelled");
            newTimes.cancel();
        }

        if (!newTimes.timesIncreasing()) {
            LOG.info("TripTimes are non-increasing after applying SIRI delay propagation - LineRef {}, TripId {}.", journey.getLineRef().getValue(), tripId);
            return null;
        }

        if (newTimes.getNumStops() != pattern.stopPattern.stops.length) {
            return null;
        }

        LOG.debug("A valid TripUpdate object was applied using the Timetable class update method.");
        return newTimes;
    }


    /**
     * Apply the SIRI ET to the appropriate TripTimes from this Timetable.
     * Calculate new stoppattern based on single stop cancellations
     *
     * @param journey    SIRI-ET EstimatedVehicleJourney
     * @param graphIndex
     * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
     * with the id specified in the trip descriptor of the TripUpdate; null if something
     * went wrong
     */
    public List<Stop> createModifiedStops(EstimatedVehicleJourney journey, GraphIndex graphIndex) {
        if (journey == null) {
            return null;
        }

        EstimatedVehicleJourney.EstimatedCalls journeyEstimatedCalls = journey.getEstimatedCalls();
        EstimatedVehicleJourney.RecordedCalls journeyRecordedCalls = journey.getRecordedCalls();

        if (journeyEstimatedCalls == null) {
            return null;
        }

        List<EstimatedCall> estimatedCalls = journeyEstimatedCalls.getEstimatedCalls();

        List<RecordedCall> recordedCalls;
        if (journeyRecordedCalls != null) {
            recordedCalls = journeyRecordedCalls.getRecordedCalls();
        } else {
            recordedCalls = new ArrayList<>();
        }

        //Get all scheduled stops
        Stop[] stops = pattern.stopPattern.stops;

        List<Stop> modifiedStops = new ArrayList<>();

        for (int i = 0; i < stops.length; i++) {
            Stop stop = stops[i];

            boolean foundMatch = false;
            if (i < recordedCalls.size()) {
                for (RecordedCall recordedCall : recordedCalls) {

                    //Current stop is being updated
                    boolean stopsMatchById = stop.getId().getId().equals(recordedCall.getStopPointRef().getValue());

                    if (!stopsMatchById && stop.getParentStation() != null) {
                        Stop alternativeStop = graphIndex.stopForId.get(new AgencyAndId(stop.getId().getAgencyId(), recordedCall.getStopPointRef().getValue()));
                        if (alternativeStop != null && stop.getParentStation().equals(alternativeStop.getParentStation())) {
                            stopsMatchById = true;
                            stop = alternativeStop;
                        }
                    }

                    if (stopsMatchById) {
                        foundMatch = true;
                        modifiedStops.add(stop);
                        break;
                    }
                }
            } else {
                for (EstimatedCall estimatedCall : estimatedCalls) {

                    //Current stop is being updated
                    boolean stopsMatchById = stop.getId().getId().equals(estimatedCall.getStopPointRef().getValue());

                    if (!stopsMatchById && stop.getParentStation() != null) {
                        Stop alternativeStop = graphIndex.stopForId.get(new AgencyAndId(stop.getId().getAgencyId(), estimatedCall.getStopPointRef().getValue()));
                        if (alternativeStop != null && stop.getParentStation().equals(alternativeStop.getParentStation())) {
                            stopsMatchById = true;
                            stop = alternativeStop;
                        }
                    }

                    if (stopsMatchById) {
                        foundMatch = true;
                        modifiedStops.add(stop);
                        break;
                    }
                }
            }
            if (!foundMatch) {
                modifiedStops.add(stop);
            }
        }


        return modifiedStops;
    }

    /**
     * Apply the SIRI ET to the appropriate TripTimes from this Timetable.
     * Calculate new stoppattern based on single stop cancellations
     *
     * @param oldTimes
     * @param journey    SIRI-ET EstimatedVehicleJourney
     * @param trip
     * @param graphIndex
     * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
     * with the id specified in the trip descriptor of the TripUpdate; null if something
     * went wrong
     */
    public List<StopTime> createModifiedStopTimes(TripTimes oldTimes, EstimatedVehicleJourney journey, Trip trip, GraphIndex graphIndex) {
        if (journey == null) {
            return null;
        }

        EstimatedVehicleJourney.EstimatedCalls journeyCalls = journey.getEstimatedCalls();

        if (journeyCalls == null) {
            return null;
        }

        List<EstimatedCall> estimatedCalls = journeyCalls.getEstimatedCalls();

        List<Stop> stops = createModifiedStops(journey, graphIndex);

        List<StopTime> modifiedStops = new ArrayList<>();

        ZonedDateTime departureDate = null;
        int numberOfRecordedCalls = (journey.getRecordedCalls() != null && journey.getRecordedCalls().getRecordedCalls() != null) ? journey.getRecordedCalls().getRecordedCalls().size() : 0;
        Set<Object> alreadyVisited = new HashSet<>();
        // modify updated stop-times
        for (int i = 0; i < stops.size(); i++) {
            Stop stop = stops.get(i);

            final StopTime stopTime = new StopTime();
            stopTime.setStop(stop);
            stopTime.setTrip(trip);
            stopTime.setStopSequence(i);
            stopTime.setDropOffType(pattern.stopPattern.dropoffs[i]);
            stopTime.setPickupType(pattern.stopPattern.pickups[i]);
            stopTime.setArrivalTime(oldTimes.getScheduledArrivalTime(i));
            stopTime.setDepartureTime(oldTimes.getScheduledDepartureTime(i));
            stopTime.setStopHeadsign(oldTimes.getHeadsign(i));

            // TODO: Do we need to set the StopTime.id?
            //stopTime.setId(oldTimes.getStopTimeIdByIndex(i));

            boolean foundMatch = false;
            if (i >= numberOfRecordedCalls) {
                for (EstimatedCall estimatedCall : estimatedCalls) {
                    if (alreadyVisited.contains(estimatedCall)) {
                        continue;
                    }
                    if (departureDate == null) {
                        departureDate = (estimatedCall.getAimedDepartureTime() != null ? estimatedCall.getAimedDepartureTime() : estimatedCall.getAimedArrivalTime());
                    }

                    //Current stop is being updated
                    boolean stopsMatchById = stop.getId().getId().equals(estimatedCall.getStopPointRef().getValue());

                    if (!stopsMatchById && stop.getParentStation() != null) {
                        Stop alternativeStop = graphIndex.stopForId.get(new AgencyAndId(stop.getId().getAgencyId(), estimatedCall.getStopPointRef().getValue()));
                        if (alternativeStop != null && stop.getParentStation().equals(alternativeStop.getParentStation())) {
                            stopsMatchById = true;
                            stopTime.setStop(alternativeStop);
                        }

                    }

                    if (stopsMatchById) {
                        foundMatch = true;

                        CallStatusEnumeration arrivalStatus = estimatedCall.getArrivalStatus();
                        if (arrivalStatus == CallStatusEnumeration.CANCELLED) {
                            stopTime.setDropOffType(PICKDROP_NONE);
                        } else if (estimatedCall.getArrivalBoardingActivity() == ArrivalBoardingActivityEnumeration.ALIGHTING) {
                            stopTime.setDropOffType(PICKDROP_SCHEDULED);
                        } else if (estimatedCall.getArrivalBoardingActivity() == ArrivalBoardingActivityEnumeration.NO_ALIGHTING) {
                            stopTime.setDropOffType(PICKDROP_NONE);
                        } else if (estimatedCall.getArrivalBoardingActivity() == null && i == 0) {
                            //First stop - default no pickup
                            stopTime.setDropOffType(PICKDROP_NONE);
                        }

                        CallStatusEnumeration departureStatus = estimatedCall.getDepartureStatus();
                        if (departureStatus == CallStatusEnumeration.CANCELLED) {
                            stopTime.setPickupType(PICKDROP_NONE);
                        } else if (estimatedCall.getDepartureBoardingActivity() == DepartureBoardingActivityEnumeration.BOARDING) {
                            stopTime.setPickupType(PICKDROP_SCHEDULED);
                        } else if (estimatedCall.getDepartureBoardingActivity() == DepartureBoardingActivityEnumeration.NO_BOARDING) {
                            stopTime.setPickupType(PICKDROP_NONE);
                        } else if (estimatedCall.getDepartureBoardingActivity() == null && i == (stops.size()-1)) {
                            //Last stop - default no dropoff
                            stopTime.setPickupType(PICKDROP_NONE);
                        }

                        if (estimatedCall.getDestinationDisplaies() != null && !estimatedCall.getDestinationDisplaies().isEmpty()) {
                            NaturalLanguageStringStructure destinationDisplay = estimatedCall.getDestinationDisplaies().get(0);
                            stopTime.setStopHeadsign(destinationDisplay.getValue());
                        }

                        modifiedStops.add(stopTime);
                        alreadyVisited.add(estimatedCall);
                        break;
                    }
                }
            }

            if (!foundMatch) {
                modifiedStops.add(stopTime);
            }
        }

        return modifiedStops;
    }

    private int calculateSecondsSinceMidnight(ZonedDateTime departureDate, ZonedDateTime dateTime) {

        int daysBetween = 0;
        if (departureDate.getDayOfMonth() != dateTime.getDayOfMonth()) {
            ZonedDateTime midnightOnDepartureDate = departureDate.withHour(0).withMinute(0).withSecond(0);
            ZonedDateTime midnightOnCurrentStop = dateTime.withHour(0).withMinute(0).withSecond(0);
            daysBetween = (int) ChronoUnit.DAYS.between(midnightOnDepartureDate, midnightOnCurrentStop);
        }
        // If first departure was 'yesterday' - add 24h
        int daysSinceDeparture = daysBetween * (24 * 60 * 60);

        return dateTime.toLocalTime().toSecondOfDay() + daysSinceDeparture;
    }

    /**
     * Apply the TripUpdate to the appropriate TripTimes from this Timetable. The existing TripTimes
     * must not be modified directly because they may be shared with the underlying
     * scheduledTimetable, or other updated Timetables. The {@link TimetableSnapshot} performs the
     * protective copying of this Timetable. It is not done in this update method to avoid
     * repeatedly cloning the same Timetable when several updates are applied to it at once. We
     * assume here that all trips in a timetable are from the same feed, which should always be the
     * case.
     *
     * @param activity SIRI-VM VehicleActivity
     * @param timeZone time zone of trip update
     * @param tripId
     * @return new copy of updated TripTimes after TripUpdate has been applied on TripTimes of trip
     * with the id specified in the trip descriptor of the TripUpdate; null if something
     * went wrong
     */
    public TripTimes createUpdatedTripTimes(Graph graph, VehicleActivityStructure activity, TimeZone timeZone, AgencyAndId tripId) {
        if (activity == null) {
            return null;
        }

        MonitoredVehicleJourneyStructure mvj = activity.getMonitoredVehicleJourney();


        int tripIndex = getTripIndex(tripId);
        if (tripIndex == -1) {
            LOG.trace("tripId {} not found in pattern.", tripId);
            return null;
        }

        final TripTimes existingTripTimes = getTripTimes(tripIndex);
        TripTimes newTimes = new TripTimes(existingTripTimes);


        MonitoredCallStructure update = mvj.getMonitoredCall();
        if (update == null) {
            return null;
        }
        final List<Stop> stops = pattern.getStops();

        VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney = activity.getMonitoredVehicleJourney();

        Duration delay = null;
        if (monitoredVehicleJourney != null) {
            delay = monitoredVehicleJourney.getDelay();
            int updatedDelay = 0;
            if (delay != null) {
                updatedDelay = delay.getSign() * (delay.getHours() * 3600 + delay.getMinutes() * 60 + delay.getSeconds());
            }

            MonitoredCallStructure monitoredCall = monitoredVehicleJourney.getMonitoredCall();
            if (monitoredCall != null && monitoredCall.getStopPointRef() != null) {
                boolean matchFound = false;

                int arrivalDelay = 0;
                int departureDelay = 0;

                for (int index = 0; index < newTimes.getNumStops(); ++index) {
                    if (!matchFound) {
                        // Delay is set on a single stop at a time. When match is found - propagate delay on all following stops
                        final Stop stop = stops.get(index);

                        matchFound = stop.getId().getId().equals(monitoredCall.getStopPointRef().getValue());

                        if (!matchFound && stop.getParentStation() != null) {
                            AgencyAndId alternativeId = new AgencyAndId(stop.getId().getAgencyId(), monitoredCall.getStopPointRef().getValue());
                            Stop alternativeStop = graph.index.stopForId.get(alternativeId);
                            if (alternativeStop != null && alternativeStop.getParentStation() != null) {
                                matchFound = stop.getParentStation().equals(alternativeStop.getParentStation());
                            }
                        }


                        if (matchFound) {
                            arrivalDelay = departureDelay = updatedDelay;
                        } else {
                            /*
                             * If updated delay is less than previously set delay, the existing delay needs to be adjusted to avoid
                             * non-increasing times causing updates to be rejected. Will only affect historical data.
                             */
                            arrivalDelay = Math.min(existingTripTimes.getArrivalDelay(index), updatedDelay);
                            departureDelay =  Math.min(existingTripTimes.getDepartureDelay(index), updatedDelay);
                        }
                    }
                    newTimes.updateArrivalDelay(index, arrivalDelay);
                    newTimes.updateDepartureDelay(index, departureDelay);
                }
            }
        }

        if (!newTimes.timesIncreasing()) {
            LOG.info("TripTimes are non-increasing after applying SIRI delay propagation - delay: {}", delay);
            return null;
        }

        //If state is already MODIFIED - keep existing state
        if (newTimes.getRealTimeState() != RealTimeState.MODIFIED) {
            // Make sure that updated trip times have the correct real time state
            newTimes.setRealTimeState(RealTimeState.UPDATED);
        }

        return newTimes;
    }

    /**
     * Add a trip to this Timetable. The Timetable must be analyzed, compacted, and indexed
     * any time trips are added, but this is not done automatically because it is time consuming
     * and should only be done once after an entire batch of trips are added.
     * Note that the trip is not added to the enclosing pattern here, but in the pattern's wrapper function.
     * Here we don't know if it's a scheduled trip or a realtime-added trip.
     */
    public void addTripTimes(TripTimes tt) {
        tripTimes.add(tt);
    }

    /**
     * Add a frequency entry to this Timetable. See addTripTimes method. Maybe Frequency Entries should
     * just be TripTimes for simplicity.
     */
    public void addFrequencyEntry(FrequencyEntry freq) {
        frequencyEntries.add(freq);
    }

    /**
     * Check that all dwell times at the given stop are zero, which allows removing the dwell edge.
     * TODO we should probably just eliminate dwell-deletion. It won't be important if we get rid of transit edges.
     */
    boolean allDwellsZero(int hopIndex) {
        for (TripTimes tt : tripTimes) {
            if (tt.getDwellTime(hopIndex) != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the shortest possible running time for this stop
     */
    public int getBestRunningTime(int stopIndex) {
        return minRunningTimes[stopIndex];
    }

    /**
     * Returns the shortest possible dwell time at this stop
     */
    public int getBestDwellTime(int stopIndex) {
        if (minDwellTimes == null) {
            return 0;
        }
        return minDwellTimes[stopIndex];
    }

    public boolean isValidFor(ServiceDate serviceDate) {
        return this.serviceDate == null || this.serviceDate.equals(serviceDate);
    }

    /**
     * Find and cache service codes. Duplicates information in trip.getServiceId for optimization.
     */
    // TODO maybe put this is a more appropriate place
    public void setServiceCodes(Map<AgencyAndId, Integer> serviceCodes) {
        for (TripTimes tt : this.tripTimes) {
            tt.serviceCode = serviceCodes.get(tt.trip.getServiceId());
        }
        // Repeated code... bad sign...
        for (FrequencyEntry freq : this.frequencyEntries) {
            TripTimes tt = freq.tripTimes;
            tt.serviceCode = serviceCodes.get(tt.trip.getServiceId());
        }
    }

} 
