package raf.gdrive.components;

import raf.gdrive.comparator.ComparatorFactory;
import storage.components.StorageFilter;

import com.google.api.services.drive.model.File;

import java.util.Collections;
import java.util.List;

public class GoogleDriveFilter implements StorageFilter<File> {
    private final GoogleDriveStorageUtils googleDriveStorageUtils;
    private final ComparatorFactory comparatorFactory;

    public GoogleDriveFilter(){
        this.googleDriveStorageUtils = new GoogleDriveStorageUtils();
        this.comparatorFactory = new ComparatorFactory();
    }

    @Override
    public String filterData(List<File> fileList, String filter) {

        boolean nameFlag = false;
        boolean lastModifiedTimeFlag = false;
        boolean creationTime = false;
        boolean size = false;
        StringBuilder result = new StringBuilder();

        if(filter.contains("n")) nameFlag = true;
        if(filter.contains("mt")) lastModifiedTimeFlag = true;
        if(filter.contains("ct")) creationTime = true;
        if(filter.contains("si")) size = true;
        if(filter.contains("asc")) fileList.sort(comparatorFactory.getComparator(filter.substring(0, 1)));
        if(filter.contains("desc")) fileList.sort(Collections.reverseOrder(comparatorFactory.getComparator(filter.substring(0, 1))));
        if(filter.contains("all") || filter.isEmpty()){
            fileList.sort(comparatorFactory.getComparator("n"));
            nameFlag = true;
            lastModifiedTimeFlag = true;
            creationTime = true;
            size = true;
        }

        for(File file : fileList){
            if(nameFlag) result.append(file.getName()).append(" ");
            if(lastModifiedTimeFlag) result.append(file.getModifiedTime()).append(" ");
            if(creationTime) result.append(file.getCreatedTime()).append(" ");
            if(size){
                if(file.getMimeType().equals("application/vnd.google-apps.folder")) result.append(googleDriveStorageUtils.getSizeOf(file.getId()));
                else result.append(file.getSize());
            }
            result.append("\n");
        }

        if(result.isEmpty()) return "Nothing matches parameters or empty!";
        else return result.toString().trim();
    }
}
