package de.schliz.cerbosjooq.internal;

import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand;
import org.jooq.Condition;
import de.schliz.cerbosjooq.Mapper;

public final class Walker {

    private final Mapper mapper;
    private LambdaScope scope; // null at top level

    public Walker(Mapper mapper) {
        this.mapper = mapper;
    }

    public Condition walk(Operand operand) {
        throw new UnsupportedOperationException("not implemented");
    }
}
