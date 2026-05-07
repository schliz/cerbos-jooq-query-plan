package de.schliz.cerbosjooq.internal;

import java.util.List;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand;

public final class Canonicalize {
    private Canonicalize() {}

    public record BinaryOperands(Operand variable, Operand value) {}

    public static BinaryOperands canonicalize(List<Operand> ops, String op) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static String flipCompare(String op) {
        throw new UnsupportedOperationException("not implemented");
    }
}
