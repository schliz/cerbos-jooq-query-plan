/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026-Present Christian Schliz <opensource@foxat.de>
 */
 
package de.schliz.cerbosjooq.internal;

import de.schliz.cerbosjooq.AttributeMapper;
import de.schliz.cerbosjooq.RelationMapping;

public record LambdaScope(String varName, RelationMapping primary, LambdaScope parent /* nullable */) {

    public Object resolve(String reference, AttributeMapper outer) {
        throw new UnsupportedOperationException("not implemented");
    }
}
