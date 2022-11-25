package raf.gdrive.comparator;

import com.google.api.services.drive.model.File;
import java.util.Comparator;

public class ComparatorFactory {

    public static Comparator<File> getComparator(String type){
        if(type == null || type.isEmpty()) return null;
        else if(type.equals("n")) return new GoogleDriveSortByName();
        else if(type.equals("c")) return new GoogleDriveSortByCreationTime();
        else if(type.equals("m")) return new GoogleDriveSortByLastModifiedTime();
        else if(type.equals("s")) return new GoogleDriveSortBySize();

        return null;
    }
}
