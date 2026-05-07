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

class StringOpsTest {

    private static final AttributeMapper MAPPER = AttributeMapper.builder()
            .field("request.resource.attr.department", TestSchema.RESOURCES_DEPT)
            .build();

    @Test
    void contains_substring() {
        var plan = conditional(expr("contains", variable("request.resource.attr.department"), value("eng")));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).containsIgnoringCase("like");
            assertThat(sql).contains("'eng'");
            assertThat(sql).contains("'%' ||");
            assertThat(sql).contains("|| '%'");
        });
    }

    @Test
    void startsWith_prefix() {
        var plan = conditional(expr("startsWith", variable("request.resource.attr.department"), value("eng")));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).containsIgnoringCase("like");
            assertThat(sql).contains("'eng'");
            assertThat(sql).contains("|| '%'");
            assertThat(sql).doesNotContain("'%' ||");
        });
    }

    @Test
    void endsWith_suffix() {
        var plan = conditional(expr("endsWith", variable("request.resource.attr.department"), value("eng")));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).containsIgnoringCase("like");
            assertThat(sql).contains("'eng'");
            assertThat(sql).contains("'%' ||");
            assertThat(sql).doesNotContain("|| '%'");
        });
    }

    @Test
    void contains_nonStringThrows() {
        var plan = conditional(expr("contains", variable("request.resource.attr.department"), value(42)));

        assertThatThrownBy(() -> QueryPlanAdapter.adapt(plan, MAPPER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contains");
    }
}
