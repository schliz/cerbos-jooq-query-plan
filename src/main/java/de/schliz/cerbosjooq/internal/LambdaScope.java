package de.schliz.cerbosjooq.internal;

import de.schliz.cerbosjooq.AttributeMapper;
import de.schliz.cerbosjooq.RelationMapping;

public record LambdaScope(
        String varName,
        RelationMapping primary,
        LambdaScope parent /* nullable */) {

    public Object resolve(String reference, AttributeMapper outer) {
        throw new UnsupportedOperationException("not implemented");
    }
}
