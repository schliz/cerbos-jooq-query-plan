package de.schliz.cerbosjooq;

import java.util.Map;
import org.jooq.Field;
import org.jooq.Table;

public record RelationMapping(
        Cardinality cardinality,
        Table<?> table,
        Field<?> sourceColumn,
        Field<?> targetColumn,
        Field<?> defaultField, // nullable
        Map<String, MapperEntry> fields) {

    public enum Cardinality { ONE, MANY }
}
