package raf.gdrive;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;
import raf.gdrive.comparator.ComparatorFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import storage.StorageManager;
import storage.storageComponents.AbstractStorage;
import storage.storageComponents.FileExtension;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleDriveStorage extends AbstractStorage {
    private static final String APPLICATION_NAME = "Google Drive Storage App";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final NetHttpTransport HTTP_TRANSPORT;
    private static Drive service;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
        StorageManager.registerStorage(new GoogleDriveStorage(null));
    }

    public GoogleDriveStorage(String path) {
        super(path);
        getStorageChecker().setStorageUtils(new GoogleDriveStorageUtils());
        createRootDirectory(path);
    }

    @Override
    protected void createJSONConfigurationFile(String path){
        Gson gson = new Gson();
            try (FileWriter writer = new FileWriter("C:\\Users\\LUKA\\Desktop\\config.JSON")) {
                gson.toJson(getConfiguration(), writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            uploadFile(List.of("C:\\Users\\LUKA\\Desktop\\config.JSON"), path);
    }


    private static Credential getCredentials() throws IOException {
        InputStream in = GoogleDriveStorage.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleDriveStorage.HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        return credential;
    }

    public static Drive getService() {
        return service;
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
        if(!getStorageChecker().checkSize(pathId, getConfiguration())) return;
        if(!getStorageChecker().checkNumberOfFileInDirectory(pathId, getConfiguration())) return;

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
        if(!getStorageChecker().checkExtension(name, getConfiguration())) return;
        if(!getStorageChecker().checkSize(pathId, getConfiguration())) return;
        if(!getStorageChecker().checkNumberOfFileInDirectory(pathId, getConfiguration())) return;

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

            if(!getStorageChecker().checkExtension(fileName, getConfiguration())) return;
            if(!getStorageChecker().checkSize(fileInList, getConfiguration())) return;
            if(!getStorageChecker().checkNumberOfFileInDirectory(destinationId, getConfiguration())) return;

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

            if(!getStorageChecker().checkExtension(fileToCheck.getName(), getConfiguration())) return;
            if(!getStorageChecker().checkSize(destinationId, getConfiguration())) return;
            if(!getStorageChecker().checkNumberOfFileInDirectory(destinationId, getConfiguration())) return;
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

        return filterData(listOfFile, filter);
    }

    @Override
    public String getFilesFromSubdirectories(String directory, String filter) {
        List<File> listOfFile = GoogleDriveStorageUtils.getAllContent(directory);

        return filterData(listOfFile, filter);
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

        return filterData(listOfFile, filter);
    }

    @Override
    public String getFilesThatContainsStringFromDirectory(String directory, String string, String filter) {
        List<File> listOfFile = GoogleDriveStorageUtils.getContent(directory);
        listOfFile.removeIf(file -> !file.getName().contains(string));

        return filterData(listOfFile, filter);
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
        return filterData(Collections.singletonList(parent), filter);
    }

    @Override
    public String getFilesInPeriod(String directory, String from, String to, String filter) {
        List<File> listOfFile = new ArrayList<>();
        FileList result;
        String formatedFrom = GoogleDriveStorageUtils.formatDate(from);
        String formatedTo = GoogleDriveStorageUtils.formatDate(to);
        String fromDirectory = "'" + directory + "' in parents";

        try {
            result = service.files().list()
                    .setQ("modifiedTime > " + formatedFrom + " and modifiedTime < " + formatedTo + " and " + fromDirectory)
                    .setFields("*")
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        listOfFile.addAll(result.getFiles());

        return filterData(listOfFile, filter);
    }

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
        if(filter.contains("asc")) Collections.sort(fileList, ComparatorFactory.getComparator(filter.substring(0, 1)));
        if(filter.contains("desc")) Collections.sort(fileList, Collections.reverseOrder(ComparatorFactory.getComparator(filter.substring(0, 1))));
        if(filter.contains("all") || filter.isEmpty()){
            Collections.sort(fileList, ComparatorFactory.getComparator("n"));
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
                if(file.getMimeType().equals("application/vnd.google-apps.folder")) result.append(new GoogleDriveStorageUtils().getSizeOf(file.getId()));
                else result.append(file.getSize());
            }
            result.append("\n");
        }

        if(result.isEmpty()) return "Nothing matches parameters or empty!";
        else return result.toString().trim();
    }
}

