package de.schliz.cerbosjooq.internal;

import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand.NodeCase;
import java.util.List;

public final class OperandNormalizer {

    private OperandNormalizer() {}

    public record BinaryOperands(Operand variable, Operand value, boolean flipped) {}

    public static BinaryOperands canonicalize(List<Operand> ops, String op) {
        if (ops.size() != 2) {
            throw new IllegalArgumentException("'" + op + "' requires 2 operands, got " + ops.size());
        }
        Operand a = ops.get(0);
        Operand b = ops.get(1);
        if (a.getNodeCase() == NodeCase.VARIABLE) {
            return new BinaryOperands(a, b, false);
        }
        if (b.getNodeCase() == NodeCase.VARIABLE) {
            return new BinaryOperands(b, a, true);
        }
        throw new IllegalArgumentException("'" + op + "' requires at least one variable operand");
    }

    public static String flipCompare(String op) {
        return switch (op) {
            case "lt" -> "gt";
            case "gt" -> "lt";
            case "le" -> "ge";
            case "ge" -> "le";
            case "eq", "ne" -> op;
            default -> throw new IllegalArgumentException("not a comparison operator: " + op);
        };
    }
}
