package org.opentripplanner.routing.algorithm.astar.strategies;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.util.Set;

public class CostSearchTerminationStrategy implements SearchTerminationStrategy {

  private final double costLimit;

  public CostSearchTerminationStrategy(double costLimit) {
    this.costLimit = costLimit;
  }

  @Override
  public boolean shouldSearchTerminate(
      Set<Vertex> origin,
      Set<Vertex> target,
      State current,
      ShortestPathTree spt,
      RoutingRequest traverseOptions
  ) {
    return current.getWeight() > costLimit;
  }
}
