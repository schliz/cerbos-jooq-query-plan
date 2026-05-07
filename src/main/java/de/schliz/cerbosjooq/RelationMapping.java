/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026-Present Christian Schliz <opensource@foxat.de>
 */
 
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
        Map<String, MappingEntry> fields) {

    public enum Cardinality {
        ONE,
        MANY
    }
}
