/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026-Present Christian Schliz <opensource@foxat.de>
 */
 
package de.schliz.cerbosjooq.fixtures;

import static de.schliz.cerbosjooq.Plans.conditional;
import static de.schliz.cerbosjooq.Plans.expr;
import static de.schliz.cerbosjooq.Plans.value;
import static de.schliz.cerbosjooq.Plans.variable;
import static org.assertj.core.api.Assertions.assertThat;

import de.schliz.cerbosjooq.AttributeMapper;
import de.schliz.cerbosjooq.QueryPlanAdapter;
import de.schliz.cerbosjooq.QueryPlanResult;
import de.schliz.cerbosjooq.TestSchema;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class SimpleEqTest {

    private static final AttributeMapper MAPPER = AttributeMapper.builder()
            .field("request.resource.attr.aBool", TestSchema.RESOURCES_ABOOL)
            .field("request.resource.attr.priority", TestSchema.RESOURCES_PRIO)
            .build();

    @Test
    void eq_variable_then_value() {
        var plan = conditional(expr("eq", variable("request.resource.attr.aBool"), value(true)));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("\"resources\".\"a_bool\" = true");
        });
    }

    /** Cerbos may emit operands in either order; the value-first form must canonicalise to the same SQL. */
    @Test
    void eq_value_then_variable() {
        var plan = conditional(expr("eq", value(true), variable("request.resource.attr.aBool")));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("\"resources\".\"a_bool\" = true");
        });
    }

    /** Reversed inequality must flip the operator: {@code 5 < priority} → {@code priority > 5}. */
    @Test
    void lt_value_then_variable_flipsToGt() {
        var plan = conditional(expr("lt", value(5), variable("request.resource.attr.priority")));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("\"resources\".\"priority\" > 5");
        });
    }

    @Test
    void ge_variable_then_value() {
        var plan = conditional(expr("ge", variable("request.resource.attr.priority"), value(3)));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("\"resources\".\"priority\" >= 3");
        });
    }
}
