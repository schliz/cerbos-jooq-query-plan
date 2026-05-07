package de.schliz.cerbosjooq;

import java.util.function.Function;
import org.jooq.Field;

public sealed interface MappingEntry permits MappingEntry.FieldRef, MappingEntry.RelationRef {

    record FieldRef(
            Field<?> column,
            Function<Object, Object> coerce // nullable
    ) implements MappingEntry {}

    record RelationRef(RelationMapping mapping) implements MappingEntry {}
}
