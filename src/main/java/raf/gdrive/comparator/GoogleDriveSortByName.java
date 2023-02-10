package raf.gdrive.comparator;

import com.google.api.services.drive.model.File;
import storage.comparator.SortByName;

public class GoogleDriveSortByName implements SortByName<File> {
    @Override
    public int compare(File f1, File f2) {
        return (f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase()));
    }
}
