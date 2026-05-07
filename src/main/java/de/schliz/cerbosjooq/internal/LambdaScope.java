/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026-Present Christian Schliz <opensource@foxat.de>
 */
 
package de.schliz.cerbosjooq.internal;

import de.schliz.cerbosjooq.AttributeMapper;
import de.schliz.cerbosjooq.MappingEntry;
import de.schliz.cerbosjooq.RelationMapping;

public record LambdaScope(String varName, RelationMapping primary, LambdaScope parent /* nullable */) {

    public MappingEntry resolve(String reference, AttributeMapper outer) {
        if (reference.equals(varName)) {
            if (primary.defaultField() == null) {
                throw new IllegalArgumentException("Bare reference to lambda variable '" + varName
                        + "' requires .targetField on the relation mapping");
            }
            return new MappingEntry.FieldRef(primary.defaultField(), null);
        }
        String prefix = varName + ".";
        if (reference.startsWith(prefix)) {
            String suffix = reference.substring(prefix.length());
            MappingEntry e = primary.fields().get(suffix);
            if (e != null) return e;
            throw new IllegalArgumentException("No mapping for '" + suffix + "' on relation '" + varName + "'");
        }
        if (parent != null) return parent.resolve(reference, outer);
        return outer.resolve(reference);
    }
}
