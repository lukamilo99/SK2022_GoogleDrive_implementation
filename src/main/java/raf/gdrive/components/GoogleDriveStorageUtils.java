package raf.gdrive.components;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import storage.components.StorageUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GoogleDriveStorageUtils extends StorageUtils {
    private static Drive service;
    public GoogleDriveStorageUtils(){
        service = GoogleDriveStorage.getService();
    }

    private long getSizeOfFromDisk(String filePath){
        java.io.File file = new java.io.File(filePath);
        long length = 0;

        if(file.isFile()) {
            try {
                return Files.size(file.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        java.io.File[] files = file.listFiles();
        if (files == null || files.length == 0){
            return length;
        }
        for (java.io.File fileInFiles : files) {
            if (file.isFile()) {
                length += fileInFiles.length();
            } else {
                length += getSizeOfFromDisk(fileInFiles.getPath());
            }
        }
        return length;
    }

    @Override
    public long getSizeOf(String fileId) {
        if(fileId.contains("\\")) return getSizeOfFromDisk(fileId);

        List<File> listOfFile = getContent(fileId);
        long size = 0L;

        for(File file : listOfFile){
            if(file.getMimeType().equals("application/vnd.google-apps.folder")){
                size += getSizeOf(file.getId());
            }
            else size += file.getSize();
        }
        return size;
    }

    public static List<File> getContent(String fileId){
        String query = "'" + fileId + "' in parents";
        FileList result;

        try {
            result = service.files().list()
                    .setQ(query)
                    .setFields("*")
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result.getFiles();
    }

    public static List<File> getAllContent(String fileId){
        List<File> resultList = new ArrayList<>();
        List<File> fileList = getContent(fileId);

        for(File file : fileList){
            if(file.getMimeType().equals("application/vnd.google-apps.folder")){
                resultList.add(file);
                resultList.addAll(getAllContent(file.getId()));
            }
            else resultList.add(file);
        }
        return resultList;
    }
    //07.03.2022. 23:22:22
    //'2012-06-04T12:00:00'
    public static String formatDate(String inputDate){
        StringBuilder resultDate = new StringBuilder();
        String[] dateParts = inputDate.split("\\.");
        String[] timeParts = inputDate.substring(inputDate.lastIndexOf(".") + 1).trim().split(":");

        resultDate.append("'").append(dateParts[2]).append("-").append(dateParts[1]).append("-").append(dateParts[0]);
        resultDate.append("T").append(timeParts[0]).append(":").append(timeParts[1]).append(":").append(timeParts[2]).append("'");

        return resultDate.toString();
    }
}
