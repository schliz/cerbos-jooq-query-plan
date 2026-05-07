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

class HasIntersectionFlatTest {

    private static final AttributeMapper MAPPER = AttributeMapper.builder()
            .relation("request.resource.attr.tags", r -> r.many(TestSchema.RESOURCE_TAGS)
                    .from(TestSchema.RESOURCES_ID)
                    .to(TestSchema.RT_RESOURCE_ID)
                    .targetField(TestSchema.RT_TAG_ID))
            .build();

    @Test
    void value_in_relation() {
        var plan = conditional(expr("in", value("t1"), variable("request.resource.attr.tags")));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("exists");
            assertThat(sql).contains("from \"resource_tags\"");
            assertThat(sql).contains("\"resource_tags\".\"resource_id\" = \"resources\".\"id\"");
            assertThat(sql).contains("\"resource_tags\".\"tag_id\" = 't1'");
        });
    }

    @Test
    void has_intersection_literal_list() {
        var plan = conditional(
                expr("hasIntersection", variable("request.resource.attr.tags"), value(List.of("t1", "t2"))));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("exists");
            assertThat(sql).contains("from \"resource_tags\"");
            assertThat(sql).contains("\"resource_tags\".\"resource_id\" = \"resources\".\"id\"");
            assertThat(sql).contains("\"resource_tags\".\"tag_id\" in (");
            assertThat(sql).contains("'t1'");
            assertThat(sql).contains("'t2'");
        });
    }

    @Test
    void has_intersection_empty_list() {
        var plan = conditional(expr("hasIntersection", variable("request.resource.attr.tags"), value(List.of())));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).containsAnyOf("1 = 0", "where false");
        });
    }
}
