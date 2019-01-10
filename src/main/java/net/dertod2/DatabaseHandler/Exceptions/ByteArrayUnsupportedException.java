package net.dertod2.DatabaseHandler.Exceptions;

import net.dertod2.DatabaseHandler.Table.Column.EntryType;

public class ByteArrayUnsupportedException extends RuntimeException {
    private static final long serialVersionUID = 2672330704093983565L;

    private EntryType entryType;

    public ByteArrayUnsupportedException(EntryType entryType) {
        this.entryType = entryType;
    }

    public String getMessage() {
        return "Can't saved ByteArrays in " + this.entryType + "!";
    }
}