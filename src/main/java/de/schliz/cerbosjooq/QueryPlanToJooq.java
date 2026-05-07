package de.schliz.cerbosjooq;

import dev.cerbos.api.v1.response.Response.PlanResourcesResponse;
import dev.cerbos.sdk.PlanResourcesResult;

public final class QueryPlanToJooq {

    private QueryPlanToJooq() {}

    public static QueryPlanResult adapt(PlanResourcesResult plan, Mapper mapper) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static QueryPlanResult adapt(PlanResourcesResponse response, Mapper mapper) {
        throw new UnsupportedOperationException("not implemented");
    }
}
