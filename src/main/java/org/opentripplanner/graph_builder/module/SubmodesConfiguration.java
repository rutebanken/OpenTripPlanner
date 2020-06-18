package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.modes.TransitSubmode;
import org.opentripplanner.model.modes.TransitSubmodeConfiguration;
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
    List<TransitSubmode> transitSubmodes = new ArrayList<>();

    for (SubmodesConfig.ConfigItem configItem : submodesConfig.getConfig()) {
      transitSubmodes.add(new TransitSubmode(configItem.name, configItem.mode));
    }

    graph.setTransitSubmodeConfiguration(new TransitSubmodeConfiguration(transitSubmodes));
  }

  public void setConfig(SubmodesConfig config) {
    this.submodesConfig = config;
  }

  @Override
  public void checkInputs() {
    // No inputs
  }
}
