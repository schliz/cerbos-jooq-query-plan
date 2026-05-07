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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.schliz.cerbosjooq.AttributeMapper;
import de.schliz.cerbosjooq.QueryPlanAdapter;
import de.schliz.cerbosjooq.QueryPlanResult;
import de.schliz.cerbosjooq.TestSchema;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class AndOrNotTest {

    private static final AttributeMapper MAPPER = AttributeMapper.builder()
            .field("request.resource.attr.status", TestSchema.RESOURCES_STATUS)
            .field("request.resource.attr.dept", TestSchema.RESOURCES_DEPT)
            .field("request.resource.attr.priority", TestSchema.RESOURCES_PRIO)
            .build();

    @Test
    void andOf_two_eqs() {
        var plan = conditional(expr(
                "and",
                expr("eq", variable("request.resource.attr.status"), value("OPEN")),
                expr("eq", variable("request.resource.attr.dept"), value("eng"))));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("\"resources\".\"status\" = 'OPEN'");
            assertThat(sql).contains("\"resources\".\"department\" = 'eng'");
            assertThat(sql).contains(" and ");
        });
    }

    @Test
    void orOf_two_eqs() {
        var plan = conditional(expr(
                "or",
                expr("eq", variable("request.resource.attr.status"), value("OPEN")),
                expr("eq", variable("request.resource.attr.status"), value("PENDING"))));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains(" or ");
            assertThat(sql).contains("'OPEN'");
            assertThat(sql).contains("'PENDING'");
        });
    }

    @Test
    void notOf_eq() {
        var plan = conditional(expr("not", expr("eq", variable("request.resource.attr.status"), value("OPEN"))));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("not (");
            assertThat(sql).contains("\"resources\".\"status\" = 'OPEN'");
        });
    }

    @Test
    void nested_and_or_not() {
        var plan = conditional(expr(
                "and",
                expr(
                        "or",
                        expr("eq", variable("request.resource.attr.status"), value("OPEN")),
                        expr("eq", variable("request.resource.attr.status"), value("PENDING"))),
                expr("not", expr("eq", variable("request.resource.attr.dept"), value("legal")))));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains(" or ");
            assertThat(sql).contains(" and ");
            assertThat(sql).contains("not (");
        });
    }

    /** Empty {@code or} has no neutral element and must fail loud — empty {@code and} is the dual that returns no-op. */
    @Test
    void emptyOr_throws() {
        var plan = conditional(expr("or"));
        assertThatThrownBy(() -> QueryPlanAdapter.adapt(plan, MAPPER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty or");
    }

    /** Empty {@code and} collapses to {@code DSL.noCondition()}, which jOOQ omits from the rendered WHERE. */
    @Test
    void emptyAnd_yieldsNoCondition() {
        var plan = conditional(expr("and"));
        var result = QueryPlanAdapter.adapt(plan, MAPPER);
        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).doesNotContain("where");
        });
    }
}
