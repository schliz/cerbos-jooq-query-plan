/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026-Present Christian Schliz <opensource@foxat.de>
 */
 
package de.schliz.cerbosjooq.fixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.schliz.cerbosjooq.AttributeMapper;
import de.schliz.cerbosjooq.Plans;
import de.schliz.cerbosjooq.QueryPlanAdapter;
import de.schliz.cerbosjooq.QueryPlanResult;
import org.junit.jupiter.api.Test;

class PlanKindsTest {

    private static final AttributeMapper EMPTY_MAPPER =
            AttributeMapper.builder().build();

    @Test
    void alwaysAllowed_returnsAlwaysAllowed() {
        QueryPlanResult r = QueryPlanAdapter.adapt(Plans.alwaysAllowed(), EMPTY_MAPPER);
        assertThat(r).isInstanceOf(QueryPlanResult.AlwaysAllowed.class);
    }

    @Test
    void alwaysDenied_returnsAlwaysDenied() {
        QueryPlanResult r = QueryPlanAdapter.adapt(Plans.alwaysDenied(), EMPTY_MAPPER);
        assertThat(r).isInstanceOf(QueryPlanResult.AlwaysDenied.class);
    }

    @Test
    void conditionalWithoutCondition_throws() {
        assertThatThrownBy(() -> QueryPlanAdapter.adapt(Plans.conditionalEmpty(), EMPTY_MAPPER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no condition");
    }
}
