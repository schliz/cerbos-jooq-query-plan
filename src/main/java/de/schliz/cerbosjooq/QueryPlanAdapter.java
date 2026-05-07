package de.schliz.cerbosjooq;

import dev.cerbos.api.v1.response.Response.PlanResourcesResponse;
import dev.cerbos.sdk.PlanResourcesResult;

public final class QueryPlanAdapter {

    private QueryPlanAdapter() {}

    public static QueryPlanResult adapt(PlanResourcesResult plan, AttributeMapper mapper) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static QueryPlanResult adapt(PlanResourcesResponse response, AttributeMapper mapper) {
        throw new UnsupportedOperationException("not implemented");
    }
}
