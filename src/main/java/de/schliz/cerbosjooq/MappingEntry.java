/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026-Present Christian Schliz <opensource@foxat.de>
 */
 
package de.schliz.cerbosjooq;

import java.util.function.Function;
import org.jooq.Field;

public sealed interface MappingEntry permits MappingEntry.FieldRef, MappingEntry.RelationRef {

    record FieldRef(Field<?> column, Function<Object, Object> coerce // nullable
            ) implements MappingEntry {}

    record RelationRef(RelationMapping mapping) implements MappingEntry {}
}
