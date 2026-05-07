/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026-Present Christian Schliz <opensource@foxat.de>
 */
 
package de.schliz.cerbosjooq;

public class UnsupportedOperatorException extends RuntimeException {

    private final String operator;

    public UnsupportedOperatorException(String operator) {
        super("Unsupported operator: " + operator);
        this.operator = operator;
    }

    public String operator() {
        return operator;
    }
}
