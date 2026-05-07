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
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.Test;

class NumericPrecisionTest {

    private static final Field<Long> BIG = DSL.field(DSL.name("resources", "big"), SQLDataType.BIGINT);

    private static final AttributeMapper MAPPER =
            AttributeMapper.builder().field("request.resource.attr.big", BIG).build();

    @Test
    void rejects_bigint_above_2_53() {
        // 1.234e18 > 2^53; the policy author's literal has already been corrupted
        // by Cerbos' double-precision wire format before we ever see it.
        var plan = conditional(expr("eq", variable("request.resource.attr.big"), value(1.234567890123456789e18)));

        assertThatThrownBy(() -> QueryPlanAdapter.adapt(plan, MAPPER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2^53");
    }

    @Test
    void rejects_non_integral_value_for_integer_column() {
        var plan = conditional(expr("eq", variable("request.resource.attr.big"), value(1.5)));

        assertThatThrownBy(() -> QueryPlanAdapter.adapt(plan, MAPPER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-integral");
    }

    @Test
    void accepts_bigint_below_2_53() {
        var plan = conditional(expr("eq", variable("request.resource.attr.big"), value(42L)));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOf(QueryPlanResult.Conditional.class);
    }

    @Test
    void accepts_value_at_exactly_2_53() {
        long boundary = 1L << 53; // 9_007_199_254_740_992 — exactly representable
        var plan = conditional(expr("eq", variable("request.resource.attr.big"), value(boundary)));

        var result = QueryPlanAdapter.adapt(plan, MAPPER);

        assertThat(result).isInstanceOf(QueryPlanResult.Conditional.class);
    }
}
