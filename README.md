# cerbos-jooq-query-plan

Adapt a Cerbos `PlanResources` decision into a `org.jooq.Condition` you can
`.and(...)` into your own jOOQ `SELECT`. Push the policy filter into the
database instead of filtering row-by-row in the application.

## Install

The library publishes a `compileOnly` dependency on jOOQ — bring your own.
You also need the protobuf runtime that ships with the Cerbos SDK.

```groovy
dependencies {
    implementation 'de.schliz:cerbos-jooq-query-plan:0.1.0'
    implementation 'dev.cerbos:cerbos-sdk-java:0.+'
    implementation 'org.jooq:jooq:3.17.0'           // or any 3.17+ version
    implementation 'com.google.protobuf:protobuf-java:4.28.2'
}
```

jOOQ baseline is **3.17**. Generated SQL stays on the dialect-portable subset
of the DSL.

## Quickstart

Given a Cerbos resource policy that gates `view` on the document's owner,
status, and tag set:

```yaml
# documents.yaml
apiVersion: api.cerbos.dev/v1
resourcePolicy:
  resource: document
  version: default
  rules:
    - actions: ["view"]
      effect: EFFECT_ALLOW
      roles: ["user"]
      condition:
        match:
          all:
            of:
              - expr: request.resource.attr.status == "published"
              - expr: >-
                  request.resource.attr.owner == request.principal.id ||
                  request.resource.attr.tags.exists(t, t.name == "public")
```

The `PlanResources` call returns an AST whose leaves reference the same
attribute paths (`request.resource.attr.owner`, `…attr.status`,
`…attr.tags`), those are exactly what the `AttributeMapper` below binds to
columns and joins:

```java
import de.schliz.cerbosjooq.AttributeMapper;
import de.schliz.cerbosjooq.QueryPlanAdapter;
import de.schliz.cerbosjooq.QueryPlanResult;
import dev.cerbos.sdk.PlanResourcesResult;

// 1. Get the plan from Cerbos.
PlanResourcesResult plan = cerbosClient
        .plan(Principal.newInstance("alice").withRole("user"))
        .resource(Resource.newInstance("document"))
        .action("view");

// 2. Describe how Cerbos attribute paths map to your jOOQ schema.
AttributeMapper mapper = AttributeMapper.builder()
        .field("request.resource.attr.owner", Tables.DOCUMENTS.OWNER_ID)
        .field("request.resource.attr.status", Tables.DOCUMENTS.STATUS)
        .relation("request.resource.attr.tags", r -> r
                .many(Tables.TAGS)
                .from(Tables.DOCUMENTS.ID)
                .to(Tables.TAGS.DOCUMENT_ID)
                .field("name", Tables.TAGS.NAME))
        .build();

// 3. Adapt and dispatch on the sealed result.
QueryPlanResult result = QueryPlanAdapter.adapt(plan, mapper);

List<Document> rows = switch (result) {
    case QueryPlanResult.AlwaysAllowed a -> ctx.selectFrom(Tables.DOCUMENTS).fetch();
    case QueryPlanResult.AlwaysDenied  d -> List.of();
    case QueryPlanResult.Conditional   c -> ctx
            .selectFrom(Tables.DOCUMENTS)
            .where(c.condition())
            .fetch();
};
```

The returned `Condition` is composable: `.and(...)` it into any existing
predicate, or wrap it with your own filters.

## Supported operators

`eq`, `ne`, `lt`, `le`, `gt`, `ge`, `and`, `or`, `not`, `isSet`, `in`,
`contains`, `startsWith`, `endsWith`, `exists`, `all`, `exists_one`,
`hasIntersection` (including `hasIntersection(map(rel, c -> c.attr), [...])`).

Unsupported operators (e.g. `hierarchy`, `add`, `now`, `getField`) throw
`UnsupportedOperatorException` at adapt time.

## Failure modes

The adapter fails loud:

- Missing mapper entry → `IllegalArgumentException`
- Unknown / unsupported operator → `UnsupportedOperatorException`
- Malformed plan (lambda shape, null in non-eq comparison, etc.) →
  `IllegalArgumentException`

There is no silent `falseCondition()` fallback except for an empty `in`
list and `KIND_ALWAYS_DENIED`, which are well-defined cases.

## License

[Mozilla Public License 2.0](./LICENSE). Copyright is retained by the
original Authors.
