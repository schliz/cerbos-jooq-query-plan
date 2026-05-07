package de.schliz.cerbosjooq.fixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.schliz.cerbosjooq.AttributeMapper;
import de.schliz.cerbosjooq.Plans;
import de.schliz.cerbosjooq.QueryPlanAdapter;
import de.schliz.cerbosjooq.QueryPlanResult;
import org.junit.jupiter.api.Test;

class PlanKindsTest {

    private static final AttributeMapper EMPTY_MAPPER = AttributeMapper.builder().build();

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
