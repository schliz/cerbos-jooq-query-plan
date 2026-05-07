/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026-Present Christian Schliz <opensource@foxat.de>
 */
 
package de.schliz.cerbosjooq;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jooq.Field;
import org.jooq.Table;

/**
 * Maps Cerbos attribute paths (e.g. {@code request.resource.attr.owner}) to jOOQ columns
 * and relations. Configure with {@link #builder()}; pass the resulting instance to
 * {@link QueryPlanAdapter#adapt}.
 *
 * <p>Resolution is exact-match by default; an optional {@link Builder#fallback fallback}
 * function can be supplied to resolve dynamically. Unmapped paths throw
 * {@link IllegalArgumentException} — there is no silent fallback to {@code falseCondition()}.
 */
public final class AttributeMapper {

    private final Map<String, MappingEntry> entries;
    private final Function<String, MappingEntry> fallback; // nullable

    private AttributeMapper(Map<String, MappingEntry> entries, Function<String, MappingEntry> fallback) {
        this.entries = Map.copyOf(entries);
        this.fallback = fallback;
    }

    public MappingEntry resolve(String path) {
        MappingEntry e = entries.get(path);
        if (e != null) return e;
        if (fallback != null) {
            MappingEntry fb = fallback.apply(path);
            if (fb != null) return fb;
        }
        throw new IllegalArgumentException("No mapping for: " + path);
    }

    /** Start a new mapper. */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link AttributeMapper}. Not thread-safe. */
    public static final class Builder {
        private final Map<String, MappingEntry> entries = new LinkedHashMap<>();
        private Function<String, MappingEntry> fallback;

        /**
         * Map a Cerbos attribute path to a single jOOQ column.
         *
         * @param attrPath e.g. {@code request.resource.attr.owner}
         * @param column   the jOOQ field whose data type drives value coercion
         */
        public Builder field(String attrPath, Field<?> column) {
            entries.put(attrPath, new MappingEntry.FieldRef(column, null));
            return this;
        }

        /**
         * Map a Cerbos attribute path to a column with a custom value coercion.
         * The coercion runs before jOOQ's {@link org.jooq.DataType#convert} so callers can
         * normalise input shapes (e.g. lower-case strings, parse enums) before binding.
         *
         * @param coerce applied to each raw protobuf-derived value (Boolean / Number / String /
         *               List / Map) before jOOQ binds it; must not be {@code null}
         */
        public Builder field(String attrPath, Field<?> column, Function<Object, Object> coerce) {
            entries.put(attrPath, new MappingEntry.FieldRef(column, coerce));
            return this;
        }

        /**
         * Map a Cerbos attribute path to a related table. The {@link RelationBuilder} closure
         * configures cardinality, source/target columns, and any sub-attribute fields used
         * in lambda bodies.
         */
        public Builder relation(String attrPath, Consumer<RelationBuilder> spec) {
            RelationBuilder rb = new RelationBuilder();
            spec.accept(rb);
            entries.put(attrPath, new MappingEntry.RelationRef(rb.build()));
            return this;
        }

        /**
         * Supply a function consulted when no exact-match entry exists. Returning {@code null}
         * falls through to the standard "no mapping" {@link IllegalArgumentException}.
         */
        public Builder fallback(Function<String, MappingEntry> fn) {
            this.fallback = fn;
            return this;
        }

        /** Build the immutable mapper. The builder may not be reused after this call. */
        public AttributeMapper build() {
            return new AttributeMapper(entries, fallback);
        }
    }

    /** Configures a {@link RelationMapping} via {@link Builder#relation}. */
    public static final class RelationBuilder {
        private RelationMapping.Cardinality cardinality;
        private Table<?> table;
        private Field<?> sourceColumn;
        private Field<?> targetColumn;
        private Field<?> defaultField;
        private final Map<String, MappingEntry> fields = new LinkedHashMap<>();

        /** Single-row relation (e.g. {@code resource.attr.owner -> users}). */
        public RelationBuilder one(Table<?> table) {
            this.cardinality = RelationMapping.Cardinality.ONE;
            this.table = table;
            return this;
        }

        /** Multi-row relation (e.g. {@code resource.attr.tags -> tags}). */
        public RelationBuilder many(Table<?> table) {
            this.cardinality = RelationMapping.Cardinality.MANY;
            this.table = table;
            return this;
        }

        /** The column on the parent table that joins to this relation. */
        public RelationBuilder from(Field<?> sourceColumn) {
            this.sourceColumn = sourceColumn;
            return this;
        }

        /** The column on the related table that joins back to {@link #from}. */
        public RelationBuilder to(Field<?> targetColumn) {
            this.targetColumn = targetColumn;
            return this;
        }

        /**
         * Default scalar field on the related table. Required for {@code value in attr}
         * and {@code hasIntersection} on the relation directly (without a {@code map}).
         */
        public RelationBuilder targetField(Field<?> defaultField) {
            this.defaultField = defaultField;
            return this;
        }

        /** Sub-attribute reachable from a lambda body, e.g. {@code c.name}. */
        public RelationBuilder field(String subAttr, Field<?> column) {
            fields.put(subAttr, new MappingEntry.FieldRef(column, null));
            return this;
        }

        /** Sub-attribute with a custom value coercion. See {@link Builder#field(String, Field, Function)}. */
        public RelationBuilder field(String subAttr, Field<?> column, Function<Object, Object> coerce) {
            fields.put(subAttr, new MappingEntry.FieldRef(column, coerce));
            return this;
        }

        /** Nested relation reachable from a lambda body (multi-level traversal). */
        public RelationBuilder relation(String subAttr, Consumer<RelationBuilder> nested) {
            RelationBuilder rb = new RelationBuilder();
            nested.accept(rb);
            fields.put(subAttr, new MappingEntry.RelationRef(rb.build()));
            return this;
        }

        RelationMapping build() {
            if (cardinality == null || table == null || sourceColumn == null || targetColumn == null) {
                throw new IllegalStateException("RelationBuilder requires one()/many(), from(), to() to all be set");
            }
            return new RelationMapping(
                    cardinality, table, sourceColumn, targetColumn, defaultField, Map.copyOf(fields));
        }
    }
}
