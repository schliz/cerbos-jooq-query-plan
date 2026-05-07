/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026-Present Christian Schliz <opensource@foxat.de>
 */
 
package de.schliz.cerbosjooq;

import de.schliz.cerbosjooq.internal.OperandVisitor;
import de.schliz.cerbosjooq.internal.Validator;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand.NodeCase;
import dev.cerbos.api.v1.response.Response.PlanResourcesResponse;
import dev.cerbos.sdk.PlanResourcesResult;
import org.jooq.Condition;

public final class QueryPlanAdapter {

    private QueryPlanAdapter() {}

    public static QueryPlanResult adapt(PlanResourcesResult plan, AttributeMapper mapper) {
        return adapt(plan.getRaw(), mapper);
    }

    public static QueryPlanResult adapt(PlanResourcesResponse response, AttributeMapper mapper) {
        PlanResourcesFilter filter = response.getFilter();
        return switch (filter.getKind()) {
            case KIND_ALWAYS_ALLOWED -> new QueryPlanResult.AlwaysAllowed();
            case KIND_ALWAYS_DENIED -> new QueryPlanResult.AlwaysDenied();
            case KIND_CONDITIONAL -> {
                Operand cond = filter.getCondition();
                if (cond.getNodeCase() == NodeCase.NODE_NOT_SET) {
                    throw new IllegalArgumentException("Conditional plan has no condition");
                }
                Validator.check(cond);
                Condition c = new OperandVisitor(mapper).walk(cond);
                yield new QueryPlanResult.Conditional(c);
            }
            case KIND_UNSPECIFIED, UNRECOGNIZED ->
                throw new IllegalArgumentException("Unknown filter kind: " + filter.getKind());
        };
    }
}
