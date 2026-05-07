package de.schliz.cerbosjooq.internal;

import java.util.function.Function;
import com.google.protobuf.Value;
import org.jooq.DataType;

public final class ValueConverter {
    private ValueConverter() {}

    public static Object toJava(Value v) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static Object coerce(Object raw, DataType<?> dt, Function<Object, Object> userCoerce) {
        throw new UnsupportedOperationException("not implemented");
    }
}
