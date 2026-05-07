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
import java.util.List;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class InListTest {

    private static final AttributeMapper MAPPER = AttributeMapper.builder()
            .field("request.resource.attr.status", TestSchema.RESOURCES_STATUS)
            .build();

    @Test
    void in_listOfStrings() {
        var plan =
                conditional(expr("in", variable("request.resource.attr.status"), value(List.of("draft", "published"))));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("\"resources\".\"status\" in (");
            assertThat(sql).contains("'draft'");
            assertThat(sql).contains("'published'");
        });
    }

    /** Single-value list must still render as IN (...), never collapse to eq. */
    @Test
    void in_singleValueList() {
        var plan = conditional(expr("in", variable("request.resource.attr.status"), value(List.of("draft"))));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("\"resources\".\"status\" in (");
            assertThat(sql).contains("'draft'");
        });
    }

    /** Empty list collapses to falseCondition — jOOQ renders this as 1 = 0 on H2. */
    @Test
    void in_emptyList() {
        var plan = conditional(expr("in", variable("request.resource.attr.status"), value(List.of())));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).containsAnyOf("1 = 0", "where false");
        });
    }
}
