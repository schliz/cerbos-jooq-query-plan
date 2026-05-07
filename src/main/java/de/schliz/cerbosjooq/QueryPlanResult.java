package de.schliz.cerbosjooq;

import org.jooq.Condition;

public sealed interface QueryPlanResult
        permits QueryPlanResult.AlwaysAllowed,
                QueryPlanResult.AlwaysDenied,
                QueryPlanResult.Conditional {

    record AlwaysAllowed() implements QueryPlanResult {}

    record AlwaysDenied() implements QueryPlanResult {}

    record Conditional(Condition condition) implements QueryPlanResult {}
}
