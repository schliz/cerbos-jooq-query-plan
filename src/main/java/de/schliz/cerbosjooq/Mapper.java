package de.schliz.cerbosjooq;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jooq.Field;
import org.jooq.Table;

public final class Mapper {

    private final Map<String, MapperEntry> entries;
    private final Function<String, MapperEntry> fallback; // nullable

    private Mapper(Map<String, MapperEntry> entries, Function<String, MapperEntry> fallback) {
        this.entries = Map.copyOf(entries);
        this.fallback = fallback;
    }

    public MapperEntry resolve(String path) {
        throw new UnsupportedOperationException("not implemented");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, MapperEntry> entries = new LinkedHashMap<>();
        private Function<String, MapperEntry> fallback;

        public Builder field(String attrPath, Field<?> column) {
            entries.put(attrPath, new MapperEntry.FieldRef(column, null));
            return this;
        }

        public Builder field(String attrPath, Field<?> column, Function<Object, Object> coerce) {
            entries.put(attrPath, new MapperEntry.FieldRef(column, coerce));
            return this;
        }

        public Builder relation(String attrPath, Consumer<RelationBuilder> spec) {
            RelationBuilder rb = new RelationBuilder();
            spec.accept(rb);
            entries.put(attrPath, new MapperEntry.RelationRef(rb.build()));
            return this;
        }

        public Builder fallback(Function<String, MapperEntry> fn) {
            this.fallback = fn;
            return this;
        }

        public Mapper build() {
            return new Mapper(entries, fallback);
        }
    }

    public static final class RelationBuilder {
        private RelationMapping.Cardinality cardinality;
        private Table<?> table;
        private Field<?> sourceColumn;
        private Field<?> targetColumn;
        private Field<?> defaultField;
        private final Map<String, MapperEntry> fields = new LinkedHashMap<>();

        public RelationBuilder one(Table<?> table) {
            this.cardinality = RelationMapping.Cardinality.ONE;
            this.table = table;
            return this;
        }

        public RelationBuilder many(Table<?> table) {
            this.cardinality = RelationMapping.Cardinality.MANY;
            this.table = table;
            return this;
        }

        public RelationBuilder from(Field<?> sourceColumn) {
            this.sourceColumn = sourceColumn;
            return this;
        }

        public RelationBuilder to(Field<?> targetColumn) {
            this.targetColumn = targetColumn;
            return this;
        }

        public RelationBuilder targetField(Field<?> defaultField) {
            this.defaultField = defaultField;
            return this;
        }

        public RelationBuilder field(String subAttr, Field<?> column) {
            fields.put(subAttr, new MapperEntry.FieldRef(column, null));
            return this;
        }

        public RelationBuilder field(String subAttr, Field<?> column, Function<Object, Object> coerce) {
            fields.put(subAttr, new MapperEntry.FieldRef(column, coerce));
            return this;
        }

        public RelationBuilder relation(String subAttr, Consumer<RelationBuilder> nested) {
            RelationBuilder rb = new RelationBuilder();
            nested.accept(rb);
            fields.put(subAttr, new MapperEntry.RelationRef(rb.build()));
            return this;
        }

        RelationMapping build() {
            if (cardinality == null || table == null || sourceColumn == null || targetColumn == null) {
                throw new IllegalStateException(
                        "RelationBuilder requires one()/many(), from(), to() to all be set");
            }
            return new RelationMapping(cardinality, table, sourceColumn, targetColumn, defaultField, Map.copyOf(fields));
        }
    }
}
