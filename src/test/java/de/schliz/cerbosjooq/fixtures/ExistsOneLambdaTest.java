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

class ExistsOneLambdaTest {

    private static final AttributeMapper MAPPER = AttributeMapper.builder()
            .relation("request.resource.attr.categories", r -> r.many(TestSchema.CATEGORIES)
                    .from(TestSchema.RESOURCES_ID)
                    .to(TestSchema.CAT_RESOURCE_ID)
                    .targetField(TestSchema.CAT_NAME)
                    .field("name", TestSchema.CAT_NAME)
                    .field("id", TestSchema.CAT_ID))
            .build();

    @Test
    void exists_one_with_subattr_body() {
        var plan = conditional(expr(
                "exists_one",
                variable("request.resource.attr.categories"),
                expr("lambda", expr("eq", variable("c.name"), value("finance")), variable("c"))));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOfSatisfying(QueryPlanResult.Conditional.class, c -> {
            String sql = TestSchema.h2()
                    .renderInlined(DSL.selectFrom(TestSchema.RESOURCES).where(c.condition()));
            assertThat(sql).contains("count(*)");
            assertThat(sql).contains("from \"categories\"");
            assertThat(sql).contains("\"categories\".\"resource_id\" = \"resources\".\"id\"");
            assertThat(sql).contains("\"categories\".\"name\" = 'finance'");
            assertThat(sql).contains("= 1");
        });
    }
}
