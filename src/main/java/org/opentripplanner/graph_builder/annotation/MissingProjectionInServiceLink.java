package org.opentripplanner.graph_builder.annotation;

public class MissingProjectionInServiceLink extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Creating straight line path between Quays for ServiceLink with missing projection: %s";

    final String serviceLinkId;

    public MissingProjectionInServiceLink(String serviceLinkId) {
        this.serviceLinkId = serviceLinkId;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, serviceLinkId);
    }
}
