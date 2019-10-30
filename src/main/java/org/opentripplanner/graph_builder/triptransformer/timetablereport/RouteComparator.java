package org.opentripplanner.graph_builder.triptransformer.timetablereport;

import org.opentripplanner.model.Route;

import java.util.Comparator;

class RouteComparator implements Comparator<Route> {
    @Override
    public int compare(Route o1, Route o2) {
        if(!o1.getAgency().equals(o2.getAgency())) {
            return o1.getAgency().getName().compareTo(o2.getAgency().getName());
        }
        String n1 = o1.getShortName();
        String n2 = o2.getShortName();

        if(n1.length() != n2.length()) {
            return Integer.compare(n1.length(), n2.length());
        }
        return n1.compareTo(n2);
    }
}
