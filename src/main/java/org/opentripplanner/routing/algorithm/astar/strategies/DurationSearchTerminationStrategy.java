package org.opentripplanner.routing.algorithm.astar.strategies;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.util.Set;

public class DurationSearchTerminationStrategy implements SearchTerminationStrategy {

  private final double durationInSeconds;

  public DurationSearchTerminationStrategy(double durationInSeconds) {
    this.durationInSeconds = durationInSeconds;
  }

  @Override
  public boolean shouldSearchTerminate(
      Set<Vertex> origin,
      Set<Vertex> target,
      State current,
      ShortestPathTree spt,
      RoutingRequest traverseOptions
  ) {
    return current.getElapsedTimeSeconds() > durationInSeconds;
  }
}
