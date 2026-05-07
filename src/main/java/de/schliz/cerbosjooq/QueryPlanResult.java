/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026-Present Christian Schliz <opensource@foxat.de>
 */
 
package de.schliz.cerbosjooq;

import org.jooq.Condition;

public sealed interface QueryPlanResult
        permits QueryPlanResult.AlwaysAllowed, QueryPlanResult.AlwaysDenied, QueryPlanResult.Conditional {

    record AlwaysAllowed() implements QueryPlanResult {}

    record AlwaysDenied() implements QueryPlanResult {}

    record Conditional(Condition condition) implements QueryPlanResult {}
}
