/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026-Present Christian Schliz <opensource@foxat.de>
 */
 
package de.schliz.cerbosjooq.internal;

import de.schliz.cerbosjooq.AttributeMapper;
import de.schliz.cerbosjooq.MappingEntry;
import de.schliz.cerbosjooq.RelationMapping;
import de.schliz.cerbosjooq.UnsupportedOperatorException;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand.NodeCase;
import java.util.ArrayList;
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
        return walk(operand, null);
    }

    private Condition walk(Operand operand, LambdaScope scope) {
        return switch (operand.getNodeCase()) {
            case EXPRESSION -> walkExpression(operand.getExpression(), scope);
            case VARIABLE ->
                throw new IllegalArgumentException(
                        "Bare variable not valid as a boolean expression: " + operand.getVariable());
            case VALUE -> throw new IllegalArgumentException("Bare value not valid as a boolean expression");
            case NODE_NOT_SET -> throw new IllegalArgumentException("Empty operand");
        };
    }

    private Condition walkExpression(Expression e, LambdaScope scope) {
        String op = e.getOperator();
        List<Operand> ops = e.getOperandsList();
        return switch (op) {
            case "and" -> {
                if (ops.isEmpty()) yield DSL.noCondition();
                Condition acc = walk(ops.get(0), scope);
                for (int i = 1; i < ops.size(); i++) acc = acc.and(walk(ops.get(i), scope));
                yield acc;
            }
            case "or" -> {
                if (ops.isEmpty()) throw new IllegalArgumentException("empty or");
                Condition acc = walk(ops.get(0), scope);
                for (int i = 1; i < ops.size(); i++) acc = acc.or(walk(ops.get(i), scope));
                yield acc;
            }
            case "not" -> {
                if (ops.size() != 1) throw new IllegalArgumentException("'not' requires 1 operand");
                yield walk(ops.get(0), scope).not();
            }
            case "eq", "ne", "lt", "le", "gt", "ge" -> walkComparison(op, ops, scope);
            case "isSet" -> walkIsSet(ops, scope);
            case "in" -> walkIn(ops, scope);
            case "contains", "startsWith", "endsWith" -> walkStringOp(op, ops, scope);
            case "exists" -> walkCollection(op, ops, scope);
            case "hasIntersection" -> walkHasIntersection(ops, scope);
            case "lambda" -> throw new IllegalArgumentException("lambda only valid inside a collection operator");
            case "filter", "map" ->
                throw new IllegalArgumentException("'" + op + "' returns a collection, not a boolean");
            default -> throw new UnsupportedOperatorException(op);
        };
    }

    private Condition walkComparison(String op, List<Operand> ops, LambdaScope scope) {
        OperandNormalizer.BinaryOperands canonical = OperandNormalizer.canonicalize(ops, op);
        String effective = canonical.flipped() ? OperandNormalizer.flipCompare(op) : op;

        MappingEntry.FieldRef fr = resolveField(canonical.variable().getVariable(), scope);
        Field<?> column = fr.column();
        Object raw = ValueConverter.toJava(canonical.value().getValue());

        if (raw == null) {
            return switch (effective) {
                case "eq" -> column.isNull();
                case "ne" -> column.isNotNull();
                default -> throw new IllegalArgumentException("null only allowed with eq/ne, got: " + effective);
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

    private Condition walkIsSet(List<Operand> ops, LambdaScope scope) {
        OperandNormalizer.BinaryOperands c = OperandNormalizer.canonicalize(ops, "isSet");
        Field<?> column = resolveField(c.variable().getVariable(), scope).column();
        Object raw = ValueConverter.toJava(c.value().getValue());
        if (!(raw instanceof Boolean b)) {
            throw new IllegalArgumentException("isSet requires a boolean value");
        }
        return b ? column.isNotNull() : column.isNull();
    }

    private Condition walkStringOp(String op, List<Operand> ops, LambdaScope scope) {
        OperandNormalizer.BinaryOperands c = OperandNormalizer.canonicalize(ops, op);
        MappingEntry.FieldRef fr = resolveField(c.variable().getVariable(), scope);
        Object raw = ValueConverter.toJava(c.value().getValue());
        if (!(raw instanceof String s)) {
            throw new IllegalArgumentException(op + " requires a string value");
        }
        @SuppressWarnings("unchecked")
        Field<String> col = (Field<String>) fr.column();
        return switch (op) {
            case "contains" -> col.contains(s);
            case "startsWith" -> col.startsWith(s);
            case "endsWith" -> col.endsWith(s);
            default -> throw new IllegalStateException(op);
        };
    }

    private Condition walkIn(List<Operand> ops, LambdaScope scope) {
        if (ops.size() != 2) {
            throw new IllegalArgumentException("'in' requires 2 operands, got " + ops.size());
        }
        Operand left = ops.get(0);
        Operand right = ops.get(1);

        if (left.getNodeCase() == NodeCase.VARIABLE && right.getNodeCase() == NodeCase.VALUE) {
            MappingEntry entry = resolve(left.getVariable(), scope);
            if (entry instanceof MappingEntry.FieldRef fr) {
                Object raw = ValueConverter.toJava(right.getValue());
                List<?> values = (raw instanceof List<?> l) ? l : List.of(raw);
                if (values.isEmpty()) return DSL.falseCondition();

                DataType<?> dt = fr.column().getDataType();
                List<Object> coerced = new ArrayList<>(values.size());
                for (Object v : values) {
                    coerced.add(ValueConverter.coerce(v, dt, fr.coerce()));
                }

                @SuppressWarnings({"unchecked", "rawtypes"})
                Field f = fr.column();
                return f.in(coerced);
            }
            throw new IllegalArgumentException(
                    "'in' with a relation on the left is not supported; use hasIntersection");
        }

        if (left.getNodeCase() == NodeCase.VALUE && right.getNodeCase() == NodeCase.VARIABLE) {
            MappingEntry entry = resolve(right.getVariable(), scope);
            if (entry instanceof MappingEntry.RelationRef rr) {
                return walkValueInRelation(left, rr.mapping(), right.getVariable());
            }
            throw new IllegalArgumentException(
                    "'value in attr' requires '" + right.getVariable() + "' to be a relation mapping");
        }

        throw new IllegalArgumentException("Unsupported 'in' operand shapes");
    }

    private Condition walkValueInRelation(Operand value, RelationMapping rel, String path) {
        if (rel.defaultField() == null) {
            throw new IllegalArgumentException(
                    "'value in " + path + "' requires .targetField(...) on the relation mapping");
        }
        Object raw = ValueConverter.toJava(value.getValue());
        Field<?> defField = rel.defaultField();
        DataType<?> dt = defField.getDataType();
        Object coerced = ValueConverter.coerce(raw, dt, null);
        Field<?> bound = DSL.val(coerced, dt);

        @SuppressWarnings({"unchecked", "rawtypes"})
        Field f = defField;
        return relationExists(rel, f.eq(bound));
    }

    private Condition walkHasIntersection(List<Operand> ops, LambdaScope scope) {
        if (ops.size() != 2) {
            throw new IllegalArgumentException("'hasIntersection' requires 2 operands, got " + ops.size());
        }
        Operand left = ops.get(0);
        Operand right = ops.get(1);

        if (left.getNodeCase() == NodeCase.EXPRESSION
                && "map".equals(left.getExpression().getOperator())) {
            return walkHasIntersectionWithMap(left.getExpression(), right, scope);
        }
        if (left.getNodeCase() != NodeCase.VARIABLE) {
            throw new IllegalArgumentException("'hasIntersection' requires a variable left operand");
        }
        if (right.getNodeCase() != NodeCase.VALUE) {
            throw new IllegalArgumentException("'hasIntersection' requires a literal-list right operand");
        }

        MappingEntry entry = resolve(left.getVariable(), scope);
        if (!(entry instanceof MappingEntry.RelationRef rr)) {
            throw new IllegalArgumentException(
                    "'hasIntersection' requires '" + left.getVariable() + "' to be a relation mapping");
        }
        RelationMapping rel = rr.mapping();
        if (rel.defaultField() == null) {
            throw new IllegalArgumentException("'hasIntersection' on '" + left.getVariable()
                    + "' requires .targetField(...) on the relation mapping");
        }

        Object raw = ValueConverter.toJava(right.getValue());
        List<?> values = (raw instanceof List<?> l) ? l : List.of(raw);
        if (values.isEmpty()) return DSL.falseCondition();

        Field<?> defField = rel.defaultField();
        DataType<?> dt = defField.getDataType();
        List<Object> coerced = new ArrayList<>(values.size());
        for (Object v : values) {
            coerced.add(ValueConverter.coerce(v, dt, null));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        Field f = defField;
        return relationExists(rel, f.in(coerced));
    }

    private Condition walkHasIntersectionWithMap(Expression mapExpr, Operand right, LambdaScope scope) {
        List<Operand> mapOps = mapExpr.getOperandsList();
        if (mapOps.size() != 2) {
            throw new IllegalArgumentException("'map' requires 2 operands, got " + mapOps.size());
        }
        Operand relOperand = mapOps.get(0);
        Operand lambdaOperand = mapOps.get(1);

        if (relOperand.getNodeCase() != NodeCase.VARIABLE) {
            throw new IllegalArgumentException("'map' requires a variable as its first operand");
        }
        if (lambdaOperand.getNodeCase() != NodeCase.EXPRESSION
                || !"lambda".equals(lambdaOperand.getExpression().getOperator())) {
            throw new IllegalArgumentException("'map' requires a lambda as its second operand");
        }

        Expression lambda = lambdaOperand.getExpression();
        List<Operand> lambdaOps = lambda.getOperandsList();
        if (lambdaOps.size() != 2) {
            throw new IllegalArgumentException(
                    "lambda must have shape [body, variable], got " + lambdaOps.size() + " operands");
        }
        Operand bodyOp = lambdaOps.get(0);
        Operand varOp = lambdaOps.get(1);
        if (varOp.getNodeCase() != NodeCase.VARIABLE) {
            throw new IllegalArgumentException(
                    "lambda variable operand must be a VARIABLE node, got " + varOp.getNodeCase());
        }
        if (bodyOp.getNodeCase() != NodeCase.VARIABLE) {
            throw new IllegalArgumentException(
                    "'map' projection lambda body must be a VARIABLE node, got " + bodyOp.getNodeCase());
        }
        String varName = varOp.getVariable();
        String bodyPath = bodyOp.getVariable();

        MappingEntry entry = resolve(relOperand.getVariable(), scope);
        if (!(entry instanceof MappingEntry.RelationRef rr)) {
            throw new IllegalArgumentException(
                    "'map' requires '" + relOperand.getVariable() + "' to be a relation mapping");
        }
        RelationMapping rel = rr.mapping();

        LambdaScope inner = new LambdaScope(varName, rel, scope);
        MappingEntry projected = inner.resolve(bodyPath, mapper);
        if (!(projected instanceof MappingEntry.FieldRef projectedFr)) {
            throw new IllegalArgumentException(
                    "'map' projection '" + bodyPath + "' must resolve to a field, not a relation");
        }
        Field<?> projectedField = projectedFr.column();

        if (right.getNodeCase() != NodeCase.VALUE) {
            throw new IllegalArgumentException("'hasIntersection' requires a literal-list right operand");
        }
        Object raw = ValueConverter.toJava(right.getValue());
        List<?> values = (raw instanceof List<?> l) ? l : List.of(raw);
        if (values.isEmpty()) return DSL.falseCondition();

        DataType<?> dt = projectedField.getDataType();
        List<Object> coerced = new ArrayList<>(values.size());
        for (Object v : values) {
            coerced.add(ValueConverter.coerce(v, dt, projectedFr.coerce()));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        Field f = projectedField;
        return relationExists(rel, f.in(coerced));
    }

    private Condition walkCollection(String op, List<Operand> ops, LambdaScope scope) {
        if (ops.size() != 2) {
            throw new IllegalArgumentException("'" + op + "' requires 2 operands, got " + ops.size());
        }
        Operand relOperand = ops.get(0);
        Operand lambdaOperand = ops.get(1);

        if (relOperand.getNodeCase() != NodeCase.VARIABLE) {
            throw new IllegalArgumentException("'" + op + "' requires a variable as its first operand");
        }
        if (lambdaOperand.getNodeCase() != NodeCase.EXPRESSION
                || !"lambda".equals(lambdaOperand.getExpression().getOperator())) {
            throw new IllegalArgumentException("'" + op + "' requires a lambda as its second operand");
        }

        Expression lambda = lambdaOperand.getExpression();
        List<Operand> lambdaOps = lambda.getOperandsList();
        if (lambdaOps.size() != 2) {
            throw new IllegalArgumentException(
                    "lambda must have shape [body, variable], got " + lambdaOps.size() + " operands");
        }
        Operand bodyOp = lambdaOps.get(0);
        Operand varOp = lambdaOps.get(1);
        if (varOp.getNodeCase() != NodeCase.VARIABLE) {
            throw new IllegalArgumentException(
                    "lambda variable operand must be a VARIABLE node, got " + varOp.getNodeCase());
        }
        String varName = varOp.getVariable();

        MappingEntry entry = resolve(relOperand.getVariable(), scope);
        if (!(entry instanceof MappingEntry.RelationRef rr)) {
            throw new IllegalArgumentException(
                    "'" + op + "' requires '" + relOperand.getVariable() + "' to be a relation mapping");
        }
        RelationMapping rel = rr.mapping();

        LambdaScope inner = new LambdaScope(varName, rel, scope);
        Condition body = walk(bodyOp, inner);

        return switch (op) {
            case "exists" -> relationExists(rel, body);
            default -> throw new IllegalStateException(op);
        };
    }

    private Condition relationExists(RelationMapping rel, Condition inner) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Field tgt = rel.targetColumn();
        Field<?> src = rel.sourceColumn();
        Condition join = tgt.eq(src);
        return DSL.exists(DSL.selectOne().from(rel.table()).where(join.and(inner)));
    }

    private MappingEntry resolve(String path, LambdaScope scope) {
        if (scope != null) return scope.resolve(path, mapper);
        return mapper.resolve(path);
    }

    private MappingEntry.FieldRef resolveField(String path, LambdaScope scope) {
        MappingEntry entry = resolve(path, scope);
        if (entry instanceof MappingEntry.FieldRef fr) {
            return fr;
        }
        throw new IllegalArgumentException(
                "Path '" + path + "' resolves to a relation; scalar comparison requires a field.");
    }
}
