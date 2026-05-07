package de.schliz.cerbosjooq;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public final class TestSchema {

    private TestSchema() {}

    public static final Table<Record> RESOURCES = DSL.table(DSL.name("resources"));
    public static final Field<String>  RESOURCES_ID     = DSL.field(DSL.name("resources", "id"),            SQLDataType.VARCHAR);
    public static final Field<String>  RESOURCES_OWNER  = DSL.field(DSL.name("resources", "owner_id"),      SQLDataType.VARCHAR);
    public static final Field<String>  RESOURCES_STATUS = DSL.field(DSL.name("resources", "status"),        SQLDataType.VARCHAR);
    public static final Field<Boolean> RESOURCES_ABOOL  = DSL.field(DSL.name("resources", "a_bool"),        SQLDataType.BOOLEAN);
    public static final Field<Integer> RESOURCES_PRIO   = DSL.field(DSL.name("resources", "priority"),      SQLDataType.INTEGER);
    public static final Field<String>  RESOURCES_DEPT   = DSL.field(DSL.name("resources", "department"),    SQLDataType.VARCHAR);
    public static final Field<String>  RESOURCES_OPT    = DSL.field(DSL.name("resources", "optional_str"),  SQLDataType.VARCHAR);

    public static final Table<Record> RESOURCE_TAGS = DSL.table(DSL.name("resource_tags"));
    public static final Field<String> RT_RESOURCE_ID = DSL.field(DSL.name("resource_tags", "resource_id"), SQLDataType.VARCHAR);
    public static final Field<String> RT_TAG_ID      = DSL.field(DSL.name("resource_tags", "tag_id"),      SQLDataType.VARCHAR);

    public static final Table<Record> TAGS = DSL.table(DSL.name("tags"));
    public static final Field<String> TAGS_ID   = DSL.field(DSL.name("tags", "id"),   SQLDataType.VARCHAR);
    public static final Field<String> TAGS_NAME = DSL.field(DSL.name("tags", "name"), SQLDataType.VARCHAR);

    public static final Table<Record> CATEGORIES = DSL.table(DSL.name("categories"));
    public static final Field<String> CAT_RESOURCE_ID = DSL.field(DSL.name("categories", "resource_id"), SQLDataType.VARCHAR);
    public static final Field<String> CAT_NAME        = DSL.field(DSL.name("categories", "name"),        SQLDataType.VARCHAR);
    public static final Field<String> CAT_ID          = DSL.field(DSL.name("categories", "id"),          SQLDataType.VARCHAR);

    public static final Table<Record> SUBCATEGORIES = DSL.table(DSL.name("subcategories"));
    public static final Field<String> SUB_CAT_ID = DSL.field(DSL.name("subcategories", "category_id"), SQLDataType.VARCHAR);
    public static final Field<String> SUB_NAME   = DSL.field(DSL.name("subcategories", "name"),        SQLDataType.VARCHAR);

    public static DSLContext h2() {
        return DSL.using(SQLDialect.H2);
    }
}
