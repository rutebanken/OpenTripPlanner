package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.modes.TransitModeConfiguration;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.SubmodesConfig;

import java.util.HashMap;

public class SubmodesConfiguration implements GraphBuilderModule {

  private SubmodesConfig submodesConfig;

  @Override
  public void buildGraph(
      Graph graph,
      HashMap<Class<?>, Object> extra,
      DataImportIssueStore issueStore
  ) {
    graph.setTransitModeConfiguration(new TransitModeConfiguration(submodesConfig));
  }

  public void setConfig(SubmodesConfig config) {
    this.submodesConfig = config;
  }

  @Override
  public void checkInputs() {
    // No inputs
  }
}