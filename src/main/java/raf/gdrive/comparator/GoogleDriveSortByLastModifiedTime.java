package raf.gdrive.comparator;

import com.google.api.services.drive.model.File;
import storage.comparator.SortByLastModifiedTime;

public class GoogleDriveSortByLastModifiedTime implements SortByLastModifiedTime<File> {
    @Override
    public int compare(File f1, File f2) {
        return (int) (f1.getModifiedTime().getValue() - (f2.getModifiedTime().getValue()));
    }
}
