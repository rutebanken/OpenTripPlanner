package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.modes.TransitMode;
import org.opentripplanner.model.modes.TransitModeConfiguration;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.SubmodesConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SubmodesConfiguration implements GraphBuilderModule {

  private SubmodesConfig submodesConfig;

  @Override
  public void buildGraph(
      Graph graph,
      HashMap<Class<?>, Object> extra,
      DataImportIssueStore issueStore
  ) {
    List<TransitMode> transitModes = new ArrayList<>();

    for (SubmodesConfig.ConfigItem configItem : submodesConfig.getConfig()) {
      transitModes.add(new TransitMode(configItem.name, configItem.mode));
    }

    graph.setTransitModeConfiguration(new TransitModeConfiguration(transitModes));
  }

  public void setConfig(SubmodesConfig config) {
    this.submodesConfig = config;
  }

  @Override
  public void checkInputs() {
    // No inputs
  }
}
