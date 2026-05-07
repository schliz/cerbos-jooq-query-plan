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
import de.schliz.cerbosjooq.TestSchema;
import de.schliz.cerbosjooq.UnsupportedOperatorException;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HierarchyUnsupportedTest {

    private static final AttributeMapper MAPPER = AttributeMapper.builder()
            .field("request.resource.attr.dept", TestSchema.RESOURCES_DEPT)
            .build();

    @Test
    void hierarchy_topLevel_throws() {
        var plan = conditional(expr("hierarchy", variable("request.resource.attr.dept"), value("eng.platform")));

        assertThatThrownBy(() -> QueryPlanAdapter.adapt(plan, MAPPER))
                .isInstanceOfSatisfying(UnsupportedOperatorException.class, ex -> {
                    assertThat(ex.operator()).isEqualTo("hierarchy");
                    assertThat(ex.getMessage()).contains("hierarchy");
                });
    }

    /** Validator must recurse: a buried unsupported op still fails fast, before the walker runs. */
    @Test
    void hierarchy_buriedUnderAnd_throws() {
        Operand inner = expr("hierarchy", variable("request.resource.attr.dept"), value("eng.platform"));
        Operand outer = expr("and", expr("eq", variable("request.resource.attr.dept"), value("eng")), inner);
        var plan = conditional(outer);

        assertThatThrownBy(() -> QueryPlanAdapter.adapt(plan, MAPPER))
                .isInstanceOfSatisfying(UnsupportedOperatorException.class, ex -> {
                    assertThat(ex.operator()).isEqualTo("hierarchy");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"add", "now", "getField", "size", "overlaps", "ancestorOf", "descendentOf"})
    void otherUnsupportedOperators_throw(String op) {
        var plan = conditional(expr(op, variable("request.resource.attr.dept"), value("x")));

        assertThatThrownBy(() -> QueryPlanAdapter.adapt(plan, MAPPER))
                .isInstanceOfSatisfying(UnsupportedOperatorException.class, ex -> {
                    assertThat(ex.operator()).isEqualTo(op);
                    assertThat(ex.getMessage()).contains(op);
                });
    }
}
