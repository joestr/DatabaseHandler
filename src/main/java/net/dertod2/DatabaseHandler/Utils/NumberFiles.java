package net.dertod2.DatabaseHandler.Utils;

import java.io.File;
import java.io.FilenameFilter;

public class NumberFiles implements FilenameFilter {

    public boolean accept(File dir, String name) {
        return name.matches("[0-9]+");
    }

}
