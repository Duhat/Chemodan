package com.example.chemodan;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.io.InputStream;

public class DriveManager {
    private static final String TAG = "DriveManager";
    private static DriveManager instance;

    private final Context context;
    private final Drive driveService;
    private String rootFolderId;
    private DriveAuthListener authListener;

    public interface DriveAuthListener {
        void onAuthorizationRequired(Intent authorizationIntent);
    }

    private DriveManager(Context context, Drive driveService, DriveAuthListener listener) {
        this.context = context.getApplicationContext();
        this.driveService = driveService;
        this.authListener = listener;
    }

    public static synchronized DriveManager getInstance(Context context, GoogleSignInAccount account, DriveAuthListener listener) {
        if (instance == null) {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    Collections.singleton(DriveScopes.DRIVE_FILE));

            credential.setSelectedAccount(account.getAccount());

            Drive driveService = new Drive.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("Chemodan")
                    .build();

            instance = new DriveManager(context, driveService, listener);
        }
        return instance;
    }

    public static synchronized DriveManager getInstance(Context context, Drive driveService, DriveAuthListener listener) {
        if (instance == null) {
            instance = new DriveManager(context, driveService, listener);
        }
        return instance;
    }


    public Task<String> uploadFile(InputStream inputStream, String fileName, String mimeType, String folderId) {
        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            try {
                File fileMetadata = new File();
                fileMetadata.setName(fileName);
                fileMetadata.setMimeType(mimeType);

                if (folderId != null && !folderId.isEmpty()) {
                    fileMetadata.setParents(Collections.singletonList(folderId));
                }

                AbstractInputStreamContent content = new InputStreamContent(mimeType, inputStream);

                File file = driveService.files().create(fileMetadata, content)
                        .setFields("id")
                        .execute();

                Log.d(TAG, "File uploaded with ID: " + file.getId());
                return file.getId();
            } catch (Exception e) {
                Log.e(TAG, "Error uploading file", e);
                throw e;
            }
        });
    }

    public Task<String> createFolder(String folderName, String parentFolderId) {
        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            try {
                File fileMetadata = new File();
                fileMetadata.setName(folderName);
                fileMetadata.setMimeType("application/vnd.google-apps.folder");

                if (parentFolderId != null && !parentFolderId.isEmpty()) {
                    fileMetadata.setParents(Collections.singletonList(parentFolderId));
                }

                File folder = driveService.files().create(fileMetadata)
                        .setFields("id")
                        .execute();

                Log.d(TAG, "Folder created with ID: " + folder.getId());
                return folder.getId();
            } catch (Exception e) {
                Log.e(TAG, "Error creating folder", e);
                throw e;
            }
        });
    }

    public Task<Void> deleteFile(String fileId) {
        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            try {
                driveService.files().delete(fileId).execute();
                Log.d(TAG, "Deleted file/folder with ID: " + fileId);
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Error deleting file/folder", e);
                throw e;
            }
        });
    }

    public Task<List<File>> listFiles(String folderId) {
        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            try {
                String query;
                if (folderId == null || folderId.isEmpty()) {
                    query = "'root' in parents and trashed = false";
                } else {
                    query = "'" + folderId + "' in parents and trashed = false";
                }

                FileList result = driveService.files().list()
                        .setQ(query)
                        .setFields("files(id, name, mimeType, modifiedTime)")
                        .execute();

                return result.getFiles();
            } catch (Exception e) {
                Log.e(TAG, "Error listing files", e);
                throw e;
            }
        });
    }

    public Task<String> createRootFolderIfNeeded() {
        if (rootFolderId != null && !rootFolderId.isEmpty()) {
            return Tasks.forResult(rootFolderId);
        }

        return createFolder("ChemodanRootFolder", null)
                .addOnSuccessListener(id -> rootFolderId = id)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create root folder", e));
    }

    public String getRootFolderId() {
        return rootFolderId;
    }

    public void setAuthListener(DriveAuthListener listener) {
        this.authListener = listener;
    }

    public Task<Void> deleteFolder(String folderId) {
        return deleteFile(folderId);
    }

    public interface DriveFilesListener {
        void onFilesLoaded(List<FileItem> files);
        void onError(Exception e);
    }

    public interface DriveOperationListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public Task<List<File>> loadFolders(String folderId) {
        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            try {
                String query = "mimeType='application/vnd.google-apps.folder' and trashed=false";
                if (folderId != null && !folderId.isEmpty()) {
                    query += " and '" + folderId + "' in parents";
                } else {
                    query += " and 'root' in parents";
                }

                FileList result = driveService.files().list()
                        .setQ(query)
                        .setFields("files(id, name, createdTime)")
                        .execute();

                return result.getFiles();
            } catch (Exception e) {
                Log.e(TAG, "Error loading folders", e);
                throw e;
            }
        });
    }
}