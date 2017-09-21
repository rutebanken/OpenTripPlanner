/* 
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/
package org.opentripplanner.gtfs;

import org.onebusaway2.gtfs.services.GtfsDao;
import org.onebusaway2.gtfs.services.GtfsDaoMutable;
import org.onebusaway2.gtfs.services.calendar.CalendarService;
import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.gtfs.mapping.ModelMapper;
import org.opentripplanner.routing.graph.AddBuilderAnnotation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.Deduplicator;

import java.io.File;
import java.io.IOException;

import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.createCalendarService;

/**
 * This class helps building GtfsContext and post process
 * the GtfsDao by repairing StopTimes(optional) and generating TripPatterns(optional).
 * This done in the {@link GtfsModule} in the production code.
 */
public class GtfsContextBuilder {

    private final GtfsFeedId feedId;

    private final GtfsDao dao;

    private CalendarService calendarService = null;

    private AddBuilderAnnotation addBuilderAnnotation = null;

    private Deduplicator deduplicator;

    private boolean repairStopTimesAndGenerateTripPatterns = true;

    public static GtfsContextBuilder contextBuilder(String path) throws IOException {
        GtfsImport gtfsImport = new GtfsImport(new File(path));
        GtfsFeedId feedId = gtfsImport.getFeedId();
        GtfsDao dao = ModelMapper.mapDao(gtfsImport.getDao());
        return new GtfsContextBuilder(feedId, dao);
    }

    public GtfsContextBuilder(GtfsFeedId feedId, GtfsDao dao) {
        this.feedId = feedId;
        this.dao = dao;
    }

    public GtfsContextBuilder withGraphBuilderAnnotationsAndDeduplicator(Graph graph) {
        return withAddBuilderAnnotation(graph)
                .withDeduplicator(graph.deduplicator);
    }

    public GtfsContextBuilder withAddBuilderAnnotation(AddBuilderAnnotation addBuilderAnnotation) {
        this.addBuilderAnnotation = addBuilderAnnotation;
        return this;
    }

    public GtfsContextBuilder withDeduplicator(Deduplicator deduplicator) {
        this.deduplicator = deduplicator;
        return this;
    }

    /**
     * The {@link org.opentripplanner.graph_builder.module.GtfsModule} is responsible for repairing
     * StopTimes for all trips and trip patterns generation, so turn this feature <b>off</b>
     * when using GtfsModule to load data.
     *
     * This feature is turned <b>on</b> by <em>default</em>.
     */
    public GtfsContextBuilder turnOffRepairStopTimesAndTripPatternsGeneration() {
        this.repairStopTimesAndGenerateTripPatterns = false;
        return this;
    }

    /**
     * This method will:
     * <ol>
     *     <li>repair stop-times (if enabled)</li>
     *     <li>generate TripPatterns (if enabled)</li>
     *     <li>create a new context</li>
     * </ol>
     */
    public GtfsContext build() {
        if(repairStopTimesAndGenerateTripPatterns) {
            repairStopTimesAndGenerateTripPatterns();
        }
        return new GtfsContextImpl(feedId, dao, calendarService());
    }

    /**
     * By default this method is part of the {@link #build()} method.
     * But in cases where you want to change the dao after building the
     * context, and these changes will affect the TripPatterns generation,
     * you should do the following:
     *
     * <pre>
     * GtfsContextBuilder contextBuilder = &lt;create context builder>;
     *
     * // turn off TripPatterns generation before building
     * context = contextBuilder
     *     .turnOffRepairStopTimesAndTripPatternsGeneration()
     *     .build();
     *
     * // Do you changes
     * applyChanges(context.getDao());
     *
     * // Repair StopTimes and generate TripPatterns
     * contextBuilder.repairStopTimesAndGenerateTripPatterns();
     * </pre>
     */
    public void repairStopTimesAndGenerateTripPatterns() {
        repairStopTimesForEachTrip();
        generateTripPatterns();
    }


    /* private stuff */

    private void repairStopTimesForEachTrip() {
        new RepairStopTimesForEachTripOperation(
                (GtfsDaoMutable) dao, addBuilderAnnotation()
        ).run();
    }

    private void generateTripPatterns() {
        new GenerateTripPatternsOperation(
                dao, addBuilderAnnotation(), deduplicator(), calendarService()
        ).run();
    }

    private CalendarService calendarService() {
        if (calendarService == null) {
            calendarService = createCalendarService(dao);
        }
        return calendarService;
    }

    private AddBuilderAnnotation addBuilderAnnotation() {
        if (addBuilderAnnotation == null) {
            addBuilderAnnotation = GraphBuilderAnnotation::getMessage;
        }
        return addBuilderAnnotation;
    }

    private Deduplicator deduplicator() {
        if (deduplicator == null) {
            deduplicator = new Deduplicator();
        }
        return deduplicator;
    }

    private static class GtfsContextImpl implements GtfsContext {

        private final GtfsFeedId feedId;

        private final GtfsDao dao;

        private final CalendarService calendar;

        private GtfsContextImpl(GtfsFeedId feedId, GtfsDao dao, CalendarService calendar) {
            this.feedId = feedId;
            this.dao = dao;
            this.calendar = calendar;
        }

        @Override
        public GtfsFeedId getFeedId() {
            return feedId;
        }

        @Override
        public GtfsDao getDao() {
            return dao;
        }

        @Override
        public CalendarService getCalendarService() {
            return calendar;
        }
    }

}
