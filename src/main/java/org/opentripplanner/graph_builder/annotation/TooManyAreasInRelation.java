package org.opentripplanner.graph_builder.annotation;

public class TooManyAreasInRelation extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Too many areas in relation {}";

    final long relationId;

    public TooManyAreasInRelation(long relationId) {
        this.relationId = relationId;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, relationId);
    }
}
