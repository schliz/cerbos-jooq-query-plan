/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026-Present Christian Schliz <opensource@foxat.de>
 */
 
package de.schliz.cerbosjooq;

import static de.schliz.cerbosjooq.Plans.conditional;
import static de.schliz.cerbosjooq.Plans.expr;
import static de.schliz.cerbosjooq.Plans.value;
import static de.schliz.cerbosjooq.Plans.variable;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntegrationTest {

    private Connection conn;
    private DSLContext ctx;

    @BeforeEach
    void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:h2:mem:cerbos_jooq_it;DB_CLOSE_DELAY=-1", "sa", "");
        ctx = DSL.using(conn, SQLDialect.H2);

        ctx.execute("create table \"resources\" ("
                + "\"id\" varchar primary key,"
                + "\"owner_id\" varchar,"
                + "\"status\" varchar)");
        ctx.execute("create table \"categories\" ("
                + "\"id\" varchar primary key,"
                + "\"resource_id\" varchar,"
                + "\"name\" varchar)");

        ctx.execute("insert into \"resources\" values ('r1','alice','active')");
        ctx.execute("insert into \"resources\" values ('r2','bob','active')");
        ctx.execute("insert into \"resources\" values ('r3','alice','archived')");
        ctx.execute("insert into \"resources\" values ('r4','carol','active')");

        ctx.execute("insert into \"categories\" values ('c1','r1','finance')");
        ctx.execute("insert into \"categories\" values ('c2','r1','public')");
        ctx.execute("insert into \"categories\" values ('c3','r2','public')");
        ctx.execute("insert into \"categories\" values ('c4','r3','finance')");
        // r4 has no categories
    }

    @AfterEach
    void tearDown() throws Exception {
        if (conn != null) conn.close();
    }

    @Test
    void exists_lambda_filters_to_oracle_set() {
        AttributeMapper mapper = AttributeMapper.builder()
                .relation("request.resource.attr.categories", r -> r.many(TestSchema.CATEGORIES)
                        .from(TestSchema.RESOURCES_ID)
                        .to(TestSchema.CAT_RESOURCE_ID)
                        .field("name", TestSchema.CAT_NAME))
                .build();

        // Plan: exists(categories, c -> c.name == "finance")
        var plan = conditional(expr(
                "exists",
                variable("request.resource.attr.categories"),
                expr("lambda", expr("eq", variable("c.name"), value("finance")), variable("c"))));

        var result = QueryPlanAdapter.adapt(plan, mapper);

        assertThat(result).isInstanceOf(QueryPlanResult.Conditional.class);
        var cond = ((QueryPlanResult.Conditional) result).condition();

        List<String> ids = ctx.select(TestSchema.RESOURCES_ID)
                .from(TestSchema.RESOURCES)
                .where(cond)
                .orderBy(TestSchema.RESOURCES_ID)
                .fetch(TestSchema.RESOURCES_ID);

        // Oracle: row-by-row Cerbos check would admit any resource whose
        // categories contain a row with name="finance" — that's r1 and r3.
        assertThat(ids).containsExactly("r1", "r3");
    }
}
