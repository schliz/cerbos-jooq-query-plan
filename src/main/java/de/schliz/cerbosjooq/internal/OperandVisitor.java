package de.schliz.cerbosjooq.internal;

import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand;
import org.jooq.Condition;
import de.schliz.cerbosjooq.AttributeMapper;

public final class OperandVisitor {

    private final AttributeMapper mapper;
    private LambdaScope scope; // null at top level

    public OperandVisitor(AttributeMapper mapper) {
        this.mapper = mapper;
    }

    public Condition walk(Operand operand) {
        throw new UnsupportedOperationException("not implemented");
    }
}
