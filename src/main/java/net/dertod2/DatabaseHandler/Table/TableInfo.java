package net.dertod2.DatabaseHandler.Table;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TableInfo {
    /**
     * The Name of the Table in the database
     */
    String tableName();
}