package de.schliz.cerbosjooq.internal;

import de.schliz.cerbosjooq.UnsupportedOperatorException;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand.NodeCase;
import java.util.Set;

public final class Validator {

    private Validator() {}

    private static final Set<String> SUPPORTED = Set.of(
            "and", "or", "not",
            "eq", "ne", "lt", "le", "gt", "ge",
            "isSet"
    );

    public static void check(Operand op) {
        if (op.getNodeCase() == NodeCase.EXPRESSION) {
            Expression e = op.getExpression();
            if (!SUPPORTED.contains(e.getOperator())) {
                throw new UnsupportedOperatorException(e.getOperator());
            }
            for (Operand child : e.getOperandsList()) {
                check(child);
            }
        }
    }
}
