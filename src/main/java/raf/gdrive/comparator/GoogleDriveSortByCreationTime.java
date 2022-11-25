package raf.gdrive.comparator;

import com.google.api.services.drive.model.File;
import storage.storageComparator.SortByCreationTime;

public class GoogleDriveSortByCreationTime implements SortByCreationTime<File> {
    @Override
    public int compare(File f1, File f2) {
        return (int) (f1.getCreatedTime().getValue() - (f2.getCreatedTime().getValue()));
    }
}
