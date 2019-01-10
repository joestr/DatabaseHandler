package net.dertod2.DatabaseHandler.Exceptions;

public class NoTableColumnException extends RuntimeException {
    private static final long serialVersionUID = -7608612414473669807L;

    private String columnName;
    private String className;

    public NoTableColumnException(String columnName, String className) {
        this.columnName = columnName;
        this.className = className;
    }

    public String getMessage() {
        return "The Column '" + this.columnName + "' does not exist in the class '" + this.className + "'";
    }
}