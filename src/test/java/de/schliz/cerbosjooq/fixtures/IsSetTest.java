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

/** {@code isSet(f, true|false)} and {@code eq/ne(f, null)} must collapse to the same {@code IS [NOT] NULL} SQL. */
class IsSetTest {

    private static final AttributeMapper MAPPER = AttributeMapper.builder()
            .field("request.resource.attr.optional", TestSchema.RESOURCES_OPT)
            .field("request.resource.attr.priority", TestSchema.RESOURCES_PRIO)
            .build();

    @Test
    void isSet_true_rendersIsNotNull() {
        var plan = conditional(expr("isSet", variable("request.resource.attr.optional"), value(true)));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("\"resources\".\"optional_str\" is not null");
        });
    }

    @Test
    void isSet_false_rendersIsNull() {
        var plan = conditional(expr("isSet", variable("request.resource.attr.optional"), value(false)));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("\"resources\".\"optional_str\" is null");
        });
    }

    @Test
    void neNull_collapsesTo_isNotNull() {
        var plan = conditional(expr("ne", variable("request.resource.attr.optional"), value(null)));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("\"resources\".\"optional_str\" is not null");
        });
    }

    @Test
    void eqNull_collapsesTo_isNull() {
        var plan = conditional(expr("eq", variable("request.resource.attr.optional"), value(null)));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("\"resources\".\"optional_str\" is null");
        });
    }

    /** Inequality against null is meaningless in SQL and signals a malformed plan; refuse rather than emit silent nonsense. */
    @Test
    void ltNull_throws() {
        var plan = conditional(expr("lt", variable("request.resource.attr.priority"), value(null)));

        assertThatThrownBy(() -> QueryPlanAdapter.adapt(plan, MAPPER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null only allowed with eq/ne");
    }
}
