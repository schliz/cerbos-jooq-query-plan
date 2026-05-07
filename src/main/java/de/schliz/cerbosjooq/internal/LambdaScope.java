package de.schliz.cerbosjooq.internal;

import de.schliz.cerbosjooq.Mapper;
import de.schliz.cerbosjooq.RelationMapping;

public record LambdaScope(
        String varName,
        RelationMapping primary,
        LambdaScope parent /* nullable */) {

    public Object resolve(String reference, Mapper outer) {
        throw new UnsupportedOperationException("not implemented");
    }
}
