package de.schliz.cerbosjooq;

import java.util.function.Function;
import org.jooq.Field;

public sealed interface MapperEntry permits MapperEntry.FieldRef, MapperEntry.RelationRef {

    record FieldRef(
            Field<?> column,
            Function<Object, Object> coerce // nullable
    ) implements MapperEntry {}

    record RelationRef(RelationMapping mapping) implements MapperEntry {}
}
