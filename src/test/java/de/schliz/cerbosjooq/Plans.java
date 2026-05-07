package de.schliz.cerbosjooq;

import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression;
import dev.cerbos.api.v1.engine.Engine.PlanResourcesFilter.Expression.Operand;
import dev.cerbos.api.v1.response.Response.PlanResourcesResponse;
import java.util.List;
import java.util.Map;

public final class Plans {

    private Plans() {}

    public static PlanResourcesResponse alwaysAllowed() {
        return PlanResourcesResponse.newBuilder()
                .setFilter(PlanResourcesFilter.newBuilder()
                        .setKind(PlanResourcesFilter.Kind.KIND_ALWAYS_ALLOWED))
                .build();
    }

    public static PlanResourcesResponse alwaysDenied() {
        return PlanResourcesResponse.newBuilder()
                .setFilter(PlanResourcesFilter.newBuilder()
                        .setKind(PlanResourcesFilter.Kind.KIND_ALWAYS_DENIED))
                .build();
    }

    public static PlanResourcesResponse conditional(Operand condition) {
        return PlanResourcesResponse.newBuilder()
                .setFilter(PlanResourcesFilter.newBuilder()
                        .setKind(PlanResourcesFilter.Kind.KIND_CONDITIONAL)
                        .setCondition(condition))
                .build();
    }

    /** Build a conditional plan with no condition set (validation edge case). */
    public static PlanResourcesResponse conditionalEmpty() {
        return PlanResourcesResponse.newBuilder()
                .setFilter(PlanResourcesFilter.newBuilder()
                        .setKind(PlanResourcesFilter.Kind.KIND_CONDITIONAL))
                .build();
    }

    public static Operand expr(String op, Operand... operands) {
        Expression.Builder e = Expression.newBuilder().setOperator(op);
        for (Operand o : operands) {
            e.addOperands(o);
        }
        return Operand.newBuilder().setExpression(e).build();
    }

    public static Operand variable(String path) {
        return Operand.newBuilder().setVariable(path).build();
    }

    public static Operand value(Object jsonLike) {
        return Operand.newBuilder().setValue(toProtoValue(jsonLike)).build();
    }

    private static Value toProtoValue(Object v) {
        if (v == null) {
            return Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
        }
        if (v instanceof Boolean b) {
            return Value.newBuilder().setBoolValue(b).build();
        }
        if (v instanceof Number n) {
            return Value.newBuilder().setNumberValue(n.doubleValue()).build();
        }
        if (v instanceof String s) {
            return Value.newBuilder().setStringValue(s).build();
        }
        if (v instanceof List<?> list) {
            ListValue.Builder lb = ListValue.newBuilder();
            for (Object e : list) {
                lb.addValues(toProtoValue(e));
            }
            return Value.newBuilder().setListValue(lb).build();
        }
        if (v instanceof Map<?, ?> map) {
            Struct.Builder sb = Struct.newBuilder();
            map.forEach((k, mv) -> sb.putFields(String.valueOf(k), toProtoValue(mv)));
            return Value.newBuilder().setStructValue(sb).build();
        }
        throw new IllegalArgumentException("Unsupported value type: " + v.getClass());
    }
}
