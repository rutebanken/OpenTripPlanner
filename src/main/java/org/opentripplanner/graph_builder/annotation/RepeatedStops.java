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

package org.opentripplanner.graph_builder.annotation;

import gnu.trove.list.TIntList;
import org.opentripplanner.model.Trip;

public class RepeatedStops extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Trip %s visits stops repeatedly. Removed duplicates at stop sequence numbers %s.";
    
    public final Trip trip;

    public final TIntList removedStopSequences;
    
    public RepeatedStops(Trip trip, TIntList removedStopSequences){
    	this.trip = trip;
        this.removedStopSequences = removedStopSequences;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, trip.getId(), removedStopSequences);
    }

}
