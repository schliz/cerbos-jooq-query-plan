/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026-Present Christian Schliz <opensource@foxat.de>
 */
 
package de.schliz.cerbosjooq.internal;

import com.google.protobuf.Value;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.jooq.DataType;

public final class ValueConverter {

    private ValueConverter() {}

    public static Object toJava(Value v) {
        return switch (v.getKindCase()) {
            case NULL_VALUE -> null;
            case BOOL_VALUE -> v.getBoolValue();
            case NUMBER_VALUE -> v.getNumberValue(); // Double — do not narrow
            case STRING_VALUE -> v.getStringValue();
            case LIST_VALUE -> {
                List<Object> out = new ArrayList<>(v.getListValue().getValuesCount());
                for (Value e : v.getListValue().getValuesList()) {
                    out.add(toJava(e));
                }
                yield out;
            }
            case STRUCT_VALUE -> {
                Map<String, Object> out = new LinkedHashMap<>();
                v.getStructValue().getFieldsMap().forEach((k, val) -> out.put(k, toJava(val)));
                yield out;
            }
            case KIND_NOT_SET -> throw new IllegalArgumentException("Value has no kind set");
        };
    }

    public static Object coerce(Object raw, DataType<?> dt, Function<Object, Object> userCoerce) {
        Object current = raw;
        if (current instanceof String s) {
            Class<?> t = dt.getType();
            if (t == LocalDate.class) current = LocalDate.parse(s);
            else if (t == LocalDateTime.class) current = LocalDateTime.parse(s);
            else if (t == OffsetDateTime.class) current = OffsetDateTime.parse(s);
            else if (t == Instant.class) current = Instant.parse(s);
            else if (t == UUID.class) current = UUID.fromString(s);
        }
        if (userCoerce != null) {
            current = userCoerce.apply(current);
        }
        return dt.convert(current);
    }
}
