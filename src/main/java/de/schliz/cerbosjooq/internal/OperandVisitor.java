package de.schliz.cerbosjooq.internal;

import de.schliz.cerbosjooq.AttributeMapper;
import de.schliz.cerbosjooq.MappingEntry;
import de.schliz.cerbosjooq.UnsupportedOperatorException;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand.NodeCase;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.DSL;

public final class OperandVisitor {

    private final AttributeMapper mapper;

    public OperandVisitor(AttributeMapper mapper) {
        this.mapper = mapper;
    }

    public Condition walk(Operand operand) {
        return switch (operand.getNodeCase()) {
            case EXPRESSION   -> walkExpression(operand.getExpression());
            case VARIABLE     -> throw new IllegalArgumentException(
                    "Bare variable not valid as a boolean expression: " + operand.getVariable());
            case VALUE        -> throw new IllegalArgumentException(
                    "Bare value not valid as a boolean expression");
            case NODE_NOT_SET -> throw new IllegalArgumentException("Empty operand");
        };
    }

    private Condition walkExpression(Expression e) {
        String op = e.getOperator();
        List<Operand> ops = e.getOperandsList();
        return switch (op) {
            case "and" -> {
                if (ops.isEmpty()) yield DSL.noCondition();
                Condition acc = walk(ops.get(0));
                for (int i = 1; i < ops.size(); i++) acc = acc.and(walk(ops.get(i)));
                yield acc;
            }
            case "or" -> {
                if (ops.isEmpty()) throw new IllegalArgumentException("empty or");
                Condition acc = walk(ops.get(0));
                for (int i = 1; i < ops.size(); i++) acc = acc.or(walk(ops.get(i)));
                yield acc;
            }
            case "not" -> {
                if (ops.size() != 1) throw new IllegalArgumentException("'not' requires 1 operand");
                yield walk(ops.get(0)).not();
            }
            case "eq", "ne", "lt", "le", "gt", "ge" -> walkComparison(op, ops);
            case "isSet" -> walkIsSet(ops);
            default -> throw new UnsupportedOperatorException(op);
        };
    }

    private Condition walkComparison(String op, List<Operand> ops) {
        OperandNormalizer.BinaryOperands canonical = OperandNormalizer.canonicalize(ops, op);
        String effective = canonical.flipped() ? OperandNormalizer.flipCompare(op) : op;

        MappingEntry.FieldRef fr = resolveField(canonical.variable().getVariable());
        Field<?> column = fr.column();
        Object raw = ValueConverter.toJava(canonical.value().getValue());

        if (raw == null) {
            return switch (effective) {
                case "eq" -> column.isNull();
                case "ne" -> column.isNotNull();
                default -> throw new IllegalArgumentException(
                        "null only allowed with eq/ne, got: " + effective);
            };
        }

        DataType<?> dt = column.getDataType();
        Object coerced = ValueConverter.coerce(raw, dt, fr.coerce());
        Field<?> bound = DSL.val(coerced, dt);

        @SuppressWarnings({"unchecked", "rawtypes"})
        Field f = column;
        return switch (effective) {
            case "eq" -> f.eq(bound);
            case "ne" -> f.ne(bound);
            case "lt" -> f.lt(bound);
            case "le" -> f.le(bound);
            case "gt" -> f.gt(bound);
            case "ge" -> f.ge(bound);
            default -> throw new IllegalStateException(effective);
        };
    }

    private Condition walkIsSet(List<Operand> ops) {
        OperandNormalizer.BinaryOperands c = OperandNormalizer.canonicalize(ops, "isSet");
        Field<?> column = resolveField(c.variable().getVariable()).column();
        Object raw = ValueConverter.toJava(c.value().getValue());
        if (!(raw instanceof Boolean b)) {
            throw new IllegalArgumentException("isSet requires a boolean value");
        }
        return b ? column.isNotNull() : column.isNull();
    }

    private MappingEntry.FieldRef resolveField(String path) {
        MappingEntry entry = mapper.resolve(path);
        if (entry instanceof MappingEntry.FieldRef fr) {
            return fr;
        }
        throw new IllegalArgumentException(
                "Path '" + path + "' resolves to a relation; scalar comparison requires a field. "
                        + "Relation handling lands in a later phase.");
    }
}
