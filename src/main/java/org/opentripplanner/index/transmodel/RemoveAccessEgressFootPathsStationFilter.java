package org.opentripplanner.index.transmodel;

import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.Iterator;
import java.util.List;

class RemoveAccessEgressFootPathsStationFilter {
    private static final int NOT_FOUND = 0;

    private final List<GraphPath> paths;
    private final AgencyAndId fromPlace;
    private final AgencyAndId toPlace;


    static void removeAccessAndEgressFootPathsAtStation(List<GraphPath> paths, AgencyAndId from, AgencyAndId to) {
        new RemoveAccessEgressFootPathsStationFilter(paths, from, to).remove();
    }

    private RemoveAccessEgressFootPathsStationFilter(List<GraphPath> paths, AgencyAndId from, AgencyAndId to) {
        this.paths = paths;
        this.fromPlace = from;
        this.toPlace = to;
    }

    public void remove() {
        for (GraphPath path : paths) {
            removeAccessWalkingPath(path);
            removeEgressWalkingPath(path);
        }
    }

    private void removeAccessWalkingPath(GraphPath path) {
        int index = findParentStop(path.states.iterator(), fromPlace);
        path.chopOffHead(index);
    }

    private void removeEgressWalkingPath(GraphPath path) {
        // Count starting at the tail
        int tailIndex = findParentStop(path.states.descendingIterator(), toPlace);
        path.chopOffTail(tailIndex);
    }

    private int findParentStop(Iterator<State> iterator, AgencyAndId parentId) {
        if(parentId == null) return NOT_FOUND;
        int pos = 0;
        while (iterator.hasNext()) {
            State state = iterator.next();
            if(state.getVertex() instanceof TransitStop) {
                TransitStop stop = (TransitStop) state.getVertex();
                if(isChildOf(stop.getStop(), parentId)) {
                    return pos;
                }
                else return NOT_FOUND;
            }
            pos++;
        }
        return NOT_FOUND;
    }

    /**
     * This method check if a {@code quay} belongs to the given StopPlace or a MultimodalStopPlace,
     * the {@code stopIdParent}.
     */
    private boolean isChildOf(Stop quay, AgencyAndId stopIdParent) {
        if(quay.getParentStation() != null) {
            if (quay.getParentStation().equals(stopIdParent.getId())) return true;
        }
        if(quay.getMultiModalStation() != null) {
            if(quay.getMultiModalStation().equals(stopIdParent.getId())) return true;
        }
        return false;
    }

    private static AgencyAndId mapToId(String stopId) {
        if(stopId == null) return null;
        return AgencyAndId.convertFromString(stopId, ':');
    }
}
