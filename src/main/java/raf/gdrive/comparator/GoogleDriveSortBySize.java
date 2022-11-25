package raf.gdrive.comparator;

import com.google.api.services.drive.model.File;
import storage.storageComparator.SortBySize;

public class GoogleDriveSortBySize implements SortBySize<File> {
    @Override
    public int compare(File f1, File f2) {
        return Math.toIntExact(f1.getSize() - f2.getSize());
    }
}
