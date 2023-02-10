package raf.gdrive.components;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import storage.StorageManager;
import storage.components.AbstractStorage;
import storage.components.FileExtension;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleDriveStorage extends AbstractStorage {

    private static Drive service;

    static {
        StorageManager.registerStorage(new GoogleDriveStorage(null));
    }

    public GoogleDriveStorage(String path) {
        super(path);
        service = GoogleDriveInit.getService();
        setUtilsForChecker(new GoogleDriveStorageUtils());
        setFilter(new GoogleDriveFilter());
        createRootDirectory(path);
    }
    //izbaciti apsolutne putanje
    @Override
    protected void createJSONConfigurationFile(String path){
        Gson gson = new Gson();
            try (FileWriter writer = new FileWriter("C:\\Users\\Luka\\Desktop\\SK2022_GoogleDrive_implementation\\src\\main\\resources\\config.json")) {
                gson.toJson(getConfiguration(), writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            uploadFile(List.of("C:\\Users\\Luka\\Desktop\\SK2022_GoogleDrive_implementation\\src\\main\\resources\\config.json"), path);
    }

    private void createRootDirectory(String path){
        File fileMetadata = new File();
        fileMetadata.setName("My Storage");
        fileMetadata.setParents(Collections.singletonList(path));
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        try {
            File file = service.files().create(fileMetadata)
                    .setFields("*")
                    .execute();
            getConfiguration().getNumberOfFilesInDirectoryMap().put(file.getId(), Integer.MAX_VALUE);
            createJSONConfigurationFile(file.getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createDirectory(String pathId, String name, int numOfFilesInDir) {
        if(!getChecker().checkSize(pathId)) return;
        if(!getChecker().checkNumberOfFileInDirectory(pathId)) return;

        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setParents(Collections.singletonList(pathId));
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        try {
            File file = service.files().create(fileMetadata)
                    .setFields("id, name, size")
                    .execute();

            getConfiguration().getNumberOfFilesInDirectoryMap().put(file.getId(), numOfFilesInDir);
            getConfiguration().takeFromNumberOfFile(pathId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createDirectories(String pathId, int numOfDir) {
        for (int i = 1; i < numOfDir + 1; i++) {
            createDirectory(pathId, "Directory" + i, 10);
        }
    }

    @Override
    public void createFile(String pathId, String name) {
        if(!getChecker().checkExtension(name)) return;
        if(!getChecker().checkSize(pathId)) return;
        if(!getChecker().checkNumberOfFileInDirectory(pathId)) return;

        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setParents(Collections.singletonList(pathId));

        try {
            File file = service.files().create(fileMetadata).setFields("*").execute();

            getConfiguration().takeFromRemainingSize(file.getSize());
            getConfiguration().takeFromNumberOfFile(pathId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createFiles(String pathId, int numOfFiles) {
        for (int i = 1; i < numOfFiles + 1; i++) {
            createFile(pathId, "File " + i + ".TXT");
        }
    }

    @Override
    public void deleteFile(String fileId) {
        try {
            File file = service.files().get(fileId).setFields("*").execute();
            getConfiguration().addToRemainingSize(file.getSize());
            getConfiguration().addToNumberOfFile(file.getParents().get(0));
            if(file.getMimeType().equals("application/vnd.google-apps.folder")) getConfiguration().getNumberOfFilesInDirectoryMap().remove(fileId);

            service.files().delete(fileId).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDirectory(String fileId) {

    }

    @Override
    public void deleteAllFilesFromDirectory(String fileId) {
        List<File> fileList = GoogleDriveStorageUtils.getContent(fileId);

        for (File file : fileList) {
            if (!file.getMimeType().equals("application/vnd.google-apps.folder")) {
                deleteFile(file.getId());
            }
        }
    }

    @Override
    public void deleteAllDirectoriesFromParentDirectory(String fileId) {
        List<File> fileList = GoogleDriveStorageUtils.getContent(fileId);

        for (File file : fileList) {
            if (file.getMimeType().equals("application/vnd.google-apps.folder")) {
                deleteFile(file.getId());
            }
        }
    }

    @Override
    public void deleteAllFromDirectory(String fileId) {
        List<File> fileList = GoogleDriveStorageUtils.getContent(fileId);

        for (File file : fileList) {
            deleteFile(file.getId());
        }
    }

    @Override
    public void uploadFile(List<String> listOfFiles, String destinationId) {

        for (String fileInList : listOfFiles) {
            String fileName = fileInList.substring(fileInList.lastIndexOf("\\") + 1);

            if(!getChecker().checkExtension(fileName)) return;
            if(!getChecker().checkSize(fileInList)) return;
            if(!getChecker().checkNumberOfFileInDirectory(destinationId)) return;

            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(Collections.singletonList(destinationId));

            java.io.File filePath = new java.io.File(fileInList);
            FileContent mediaContent = new FileContent("", filePath);

            try {
                File file = service.files().create(fileMetadata, mediaContent)
                        .setFields("*")
                        .execute();

                getConfiguration().takeFromRemainingSize(file.getSize());
                getConfiguration().takeFromNumberOfFile(destinationId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void moveFile(String fileId, String destinationId) {
        try {
            File fileToCheck = service.files().get(fileId).setFields("name").execute();

            if(!getChecker().checkExtension(fileToCheck.getName())) return;
            if(!getChecker().checkSize(destinationId)) return;
            if(!getChecker().checkNumberOfFileInDirectory(destinationId)) return;
            File file = new File();
            file.setParents(Collections.singletonList(destinationId));

            File resultFile = service.files().copy(fileId, file)
                    .setFields("*")
                    .execute();
            getConfiguration().takeFromRemainingSize(resultFile.getSize());
            getConfiguration().takeFromNumberOfFile(destinationId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        deleteFile(fileId);
    }

    @Override
    public void saveFile(String fileId, String downloadPath) {
        java.io.File file;
        OutputStream outputStream;

        try {
            file = new java.io.File(downloadPath + "\\" + service.files().get(fileId).execute().getName());
            outputStream = new FileOutputStream(file);
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void renameFile(String fileId, String newName) {
        try {
            File file = service.files().get(fileId).setFields("name, mimeType").execute().setName(newName);
            service.files().update(fileId, file)
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFilesFromDirectory(String directory, String filter) {
        List<File> listOfFile = GoogleDriveStorageUtils.getContent(directory);

        return getFilter().filterData(listOfFile, filter);
    }

    @Override
    public String getFilesFromSubdirectories(String directory, String filter) {
        List<File> listOfFile = GoogleDriveStorageUtils.getAllContent(directory);

        return getFilter().filterData(listOfFile, filter);
    }

    @Override
    public String getFilesFromDirectoryAndSubdirectories(String s, String s1) {
        return null;
    }

    @Override
    public String getFilesWithExtensionFromDirectory(String directory, FileExtension fileExtension, String filter) {
        List<File> listOfFile = GoogleDriveStorageUtils.getContent(directory);
        listOfFile.removeIf(file -> file.getMimeType().equals("application/vnd.google-apps.folder"));
        listOfFile.removeIf(file -> !GoogleDriveStorageUtils.getFileExtension(file.getName()).equals(fileExtension));

        return getFilter().filterData(listOfFile, filter);
    }

    @Override
    public String getFilesThatContainsStringFromDirectory(String directory, String string, String filter) {
        List<File> listOfFile = GoogleDriveStorageUtils.getContent(directory);
        listOfFile.removeIf(file -> !file.getName().contains(string));

        return getFilter().filterData(listOfFile, filter);
    }

    @Override
    public boolean containsFileWithName(String directory, List<String> listOfNames) {
        List<File> listOfFile = GoogleDriveStorageUtils.getContent(directory);

        for(File file : listOfFile){
            if(listOfNames.contains(file.getName())) return true;
        }
        return false;
    }

    @Override
    public String getParentDirectoryOfFile(String fileId, String filter) {
        File file;
        File parent;
        try {
            file = service.files().get(fileId).setFields("id, parents").execute();
            parent = service.files().get(file.getParents().get(0)).setFields("*").execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return getFilter().filterData(Collections.singletonList(parent), filter);
    }

    @Override
    public String getFilesInPeriod(String directory, String from, String to, String filter) {
        FileList result;
        String formattedFrom = GoogleDriveStorageUtils.formatDate(from);
        String formattedTo = GoogleDriveStorageUtils.formatDate(to);
        String fromDirectory = "'" + directory + "' in parents";

        try {
            result = service.files().list()
                    .setQ("modifiedTime > " + formattedFrom + " and modifiedTime < " + formattedTo + " and " + fromDirectory)
                    .setFields("*")
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<File> listOfFile = new ArrayList<>(result.getFiles());

        return getFilter().filterData(listOfFile, filter);
    }
}

