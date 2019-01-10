package net.dertod2.DatabaseHandler.Table;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.dertod2.DatabaseHandler.Table.Column.ColumnType;

public abstract class TableEntry {
    protected boolean isLoadedEntry;

    /**
     * Gets the set tableName for this Class
     * 
     * @return
     */
    public String getTableName() {
        TableInfo tableInfo = getClass().getAnnotation(TableInfo.class);
        if (tableInfo != null)
            return tableInfo.tableName();
        return getClass().getName();
    }

    /**
     * Wherever this Entry was loaded out of the database or created by the user
     * 
     * @return
     */
    public boolean isLoaded() {
        return this.isLoadedEntry;
    }

    /**
     * This method will be called when succesfully and completely loaded this entry
     * out of the database.<br />
     * Can be used to handle the data afterwards
     */
    public void inform() {
    }

    /**
     * Returns an empty new Instance of this class to work for the database
     * 
     * @return
     */
    public abstract TableEntry getInstance();

    public boolean hasPrimaryKey() {
        Field[] fieldList = TableEntry.safeArrayMerge(getClass().getFields(), getClass().getDeclaredFields(),
                new Field[1]);

        for (final Field field : fieldList) {
            if (!field.isAnnotationPresent(Column.class))
                continue;
            Column columnInfo = field.getAnnotation(Column.class);
            if (columnInfo.columnType() == ColumnType.Primary)
                return true;
        }

        return false;
    }

    public Column getPrimaryKey() {
        Field[] fieldList = TableEntry.safeArrayMerge(getClass().getFields(), getClass().getDeclaredFields(),
                new Field[1]);

        for (final Field field : fieldList) {
            if (!field.isAnnotationPresent(Column.class))
                continue;
            Column column = field.getAnnotation(Column.class);
            if (column.columnType() == ColumnType.Primary)
                return column;
        }

        return null;
    }

    public boolean hasUniqueKeys() {
        Field[] fieldList = TableEntry.safeArrayMerge(getClass().getFields(), getClass().getDeclaredFields(),
                new Field[1]);

        for (final Field field : fieldList) {
            if (!field.isAnnotationPresent(Column.class))
                continue;
            Column column = field.getAnnotation(Column.class);
            if (column.columnType() == ColumnType.Unique)
                return true;
        }

        return false;
    }

    public List<Column> getUniqueKeys() {
        List<Column> resultList = new ArrayList<Column>();

        Field[] fieldList = TableEntry.safeArrayMerge(getClass().getFields(), getClass().getDeclaredFields(),
                new Field[1]);
        for (final Field field : fieldList) {
            if (!field.isAnnotationPresent(Column.class))
                continue;
            Column column = field.getAnnotation(Column.class);
            if (column.columnType() == ColumnType.Unique) {
                resultList.add(column);
            }
        }

        return resultList;
    }

    protected boolean setColumn(Column column, Object object) throws IllegalArgumentException, IllegalAccessException {
        Field[] fieldList = TableEntry.safeArrayMerge(getClass().getFields(), getClass().getDeclaredFields(),
                new Field[1]);

        for (final Field field : fieldList) {
            if (!field.isAnnotationPresent(Column.class))
                continue;
            Column fieldInfo = field.getAnnotation(Column.class);

            if (column.equals(fieldInfo)) {
                boolean wasAccessible = field.isAccessible();
                if (!field.isAccessible())
                    field.setAccessible(true);

                field.set(this, object);

                field.setAccessible(wasAccessible);

                return true;
            }
        }

        return false;
    }

    protected Object getColumn(Column column) throws IllegalArgumentException, IllegalAccessException {
        Field[] fieldList = TableEntry.safeArrayMerge(getClass().getFields(), getClass().getDeclaredFields(),
                new Field[1]);

        for (final Field field : fieldList) {
            if (!field.isAnnotationPresent(Column.class))
                continue;
            Column fieldInfo = field.getAnnotation(Column.class);

            if (fieldInfo.equals(column)) {
                boolean wasAccessible = field.isAccessible();
                if (!field.isAccessible())
                    field.setAccessible(true);

                Object results = field.get(this);

                field.setAccessible(wasAccessible);

                return results;
            }
        }

        return null;
    }

    protected Column getColumn(String columnName) {
        Map<String, Column> layoutList = this.getTableLayout();
        return layoutList.get(columnName);
    }

    protected Column getColumn(int order) {
        List<Column> layoutList = this.getPlainLayout();
        return layoutList.get(order - 1);
    }

    protected Map<Column, Object> getEntryColumns() {
        List<Field> fieldList = this.getSortedFields();
        Map<Column, Object> columnList = new HashMap<Column, Object>();

        for (final Field field : fieldList) {
            if (!field.isAnnotationPresent(Column.class))
                continue;
            final Column column = field.getAnnotation(Column.class);

            boolean wasAccessible = field.isAccessible();
            if (!field.isAccessible())
                field.setAccessible(true);

            try {
                try {
                    columnList.put(column, field.get(this));
                } catch (NullPointerException exc) {
                    columnList.put(column, null);
                }
            } catch (IllegalArgumentException exc) {
                exc.printStackTrace();
            } catch (IllegalAccessException exc) {
                exc.printStackTrace();
            }

            field.setAccessible(wasAccessible);
        }

        return columnList;
    }

    protected Map<String, Column> getTableLayout() {
        List<Field> fieldList = this.getSortedFields();

        Map<String, Column> layoutList = new HashMap<String, Column>();

        for (final Field field : fieldList) {
            final Column column = field.getAnnotation(Column.class);

            layoutList.put(column.columnName(), column);
        }

        return layoutList;
    }

    protected List<Column> getPlainLayout() {
        List<Field> fieldList = this.getSortedFields();
        List<Column> layoutList = new ArrayList<Column>();

        for (final Field field : fieldList) {
            final Column column = field.getAnnotation(Column.class);

            layoutList.add(column);
        }

        return layoutList;
    }

    private static <T> T[] safeArrayMerge(T[] first, T[] second, T[] template) {
        HashSet<T> res = new HashSet<T>();

        Collections.addAll(res, first);
        Collections.addAll(res, second);
        return res.toArray(template);
    }

    /**
     * Gets all fields of this class with the annotation {@link Column}<br />
     * if the rows contains the order attrubute the fields will be sorted
     * 
     * @param annotatiton
     * @return
     */
    private List<Field> getSortedFields() {
        Field[] fieldList = TableEntry.safeArrayMerge(getClass().getFields(), getClass().getDeclaredFields(),
                new Field[1]);
        List<Field> sortedList = new ArrayList<Field>();

        for (Field field : fieldList) {
            if (!field.isAnnotationPresent(Column.class))
                continue;
            sortedList.add(field);
        }

        sortedList.sort(new ColumnFieldSorter());
        return sortedList;
    }

    public static class ColumnFieldSorter implements Comparator<Field> {

        public int compare(Field o1, Field o2) {
            if (!o1.isAnnotationPresent(Column.class))
                return 0;
            if (!o2.isAnnotationPresent(Column.class))
                return 0;

            Column f1 = o1.getAnnotation(Column.class);
            Column f2 = o2.getAnnotation(Column.class);

            if (f1.order() != -1 && f2.order() != -1) {
                if (f1.order() > f2.order())
                    return 1;
                if (f1.order() < f2.order())
                    return -1;
            }

            return 0;
        }

    }

    public static class ColumnClassSorter implements Comparator<Column> {

        public int compare(Column o1, Column o2) {
            if (o1.order() != -1 && o2.order() != -1) {
                if (o1.order() > o2.order())
                    return 1;
                if (o1.order() < o2.order())
                    return -1;
            }

            return 0;
        }

    }
}