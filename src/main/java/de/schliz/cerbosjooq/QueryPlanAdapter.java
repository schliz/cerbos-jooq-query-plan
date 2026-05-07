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

/**
 * Entry point for adapting a Cerbos {@code PlanResources} result into a jOOQ
 * {@link Condition} that callers {@code .and(...)} into their own {@code SELECT}.
 *
 * <p>The returned {@link QueryPlanResult} is sealed: {@link QueryPlanResult.AlwaysAllowed}
 * indicates no filter is required, {@link QueryPlanResult.AlwaysDenied} indicates the
 * caller should short-circuit (e.g. return an empty result without querying), and
 * {@link QueryPlanResult.Conditional} carries the dialect-portable {@link Condition}
 * to attach to the caller's query.
 *
 * <p>Adaptation is pure: the same input plan and {@link AttributeMapper} always produce
 * an equivalent condition. Unknown operators throw {@link UnsupportedOperatorException};
 * malformed plans, missing mapper entries, and shape errors throw
 * {@link IllegalArgumentException}.
 */
public final class QueryPlanAdapter {

    private QueryPlanAdapter() {}

    /**
     * Adapt a {@link PlanResourcesResult} produced by the Cerbos SDK.
     *
     * @param plan   the plan returned from {@code CerbosBlockingClient.plan(...)}
     * @param mapper resolves Cerbos attribute paths (e.g. {@code request.resource.attr.owner})
     *               to jOOQ {@link org.jooq.Field}s and relations
     * @return a sealed {@link QueryPlanResult}
     * @throws IllegalArgumentException     if the plan is malformed or a mapping is missing
     * @throws UnsupportedOperatorException if the plan uses an operator not supported by this adapter
     */
    public static QueryPlanResult adapt(PlanResourcesResult plan, AttributeMapper mapper) {
        return adapt(plan.getRaw(), mapper);
    }

    /**
     * Adapt a raw {@link PlanResourcesResponse} protobuf message. Useful for testing or for
     * callers that obtain the response outside the SDK helper.
     *
     * @see #adapt(PlanResourcesResult, AttributeMapper)
     */
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
