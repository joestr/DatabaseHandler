package net.dertod2.DatabaseHandler.Table;

import java.util.HashMap;
import java.util.Map;

public class LoadHelper {
    protected int limit = 0;
    protected int offset = 0;

    protected Map<String, Sort> columnSorter = new HashMap<String, Sort>();

    public LoadHelper limit(int limit) {
        this.limit = limit;
        return this;
    }

    public LoadHelper limit(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
        return this;
    }

    public LoadHelper offset(int offset) {
        this.offset = offset;
        return this;
    }

    public LoadHelper column(String field) {
        this.columnSorter.put(field, Sort.ASC);
        return this;
    }

    public LoadHelper column(String field, Sort sortOrder) {
        this.columnSorter.put(field, sortOrder);
        return this;
    }

    public enum Sort {
        DESC("DESC"), ASC("ASC");

        private String sort;

        private Sort(String sort) {
            this.sort = sort;
        }

        public String getSort() {
            return this.sort;
        }
    }
}