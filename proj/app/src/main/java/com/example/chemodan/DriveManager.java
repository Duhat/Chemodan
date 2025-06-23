package com.example.chemodan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import com.example.chemodan.FileAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class DriveManager {
    private static final String TAG = "DriveManager";

    public interface DriveAuthListener {
        void onAuthorizationRequired(Intent authorizationIntent);
    }

    private final Activity activity;
    private final Drive driveService;
    private final SharedPreferences preferences;
    private final FolderAdapter folderAdapter;
    private final FileAdapter fileAdapter;
    private final DriveAuthListener authListener;

    private static final String PREF_NAME = "DrivePrefs";
    private static final String KEY_ROOT_FOLDER_ID = "root_folder_id";

    public DriveManager(Activity activity, Drive driveService,
                        FolderAdapter folderAdapter, FileAdapter fileAdapter,
                        DriveAuthListener authListener) {
        this.activity = activity;
        this.driveService = driveService;
        this.folderAdapter = folderAdapter;
        this.fileAdapter = fileAdapter;
        this.preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.authListener = authListener;
    }

    // Создание корневой папки, если еще нет
    public void createRootFolderIfNeeded() {
        String rootId = preferences.getString(KEY_ROOT_FOLDER_ID, null);
        if (rootId == null) {
            createFolder("DocStorage", null)
                    .addOnSuccessListener(folderId -> {
                        preferences.edit().putString(KEY_ROOT_FOLDER_ID, folderId).apply();
                        loadFolders(folderId);
                    })
                    .addOnFailureListener(e -> {
                        // Обработка ошибки
                    });
        } else {
            loadFolders(rootId);
        }
    }


    // Создание папки с именем и родителем
    public Task<String> createFolder(String name, String parentId) {
        Log.d(TAG, "Creating folder: " + name + ", parentId: " + parentId);
        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            try {
                File folderMetadata = new File()
                        .setName(name)
                        .setMimeType("application/vnd.google-apps.folder");

                if (parentId != null && !parentId.isEmpty()) {
                    folderMetadata.setParents(Collections.singletonList(parentId));
                }

                File folder = driveService.files().create(folderMetadata)
                        .setFields("id")
                        .execute();

                Log.d(TAG, "Folder created with ID: " + folder.getId());
                return folder.getId();
            } catch (UserRecoverableAuthIOException e) {
                Log.e(TAG, "Authorization required to create folder", e);
                activity.runOnUiThread(() -> authListener.onAuthorizationRequired(e.getIntent()));
                throw e;
            } catch (Exception e) {
                Log.e(TAG, "Error creating folder", e);
                throw e;
            }
        });
    }

    // Загрузка папок внутри папки parentId
    public void loadFolders(String parentId) {
        String queryParentId = (parentId != null && !parentId.isEmpty()) ? parentId : "root";

        Log.d(TAG, "Loading folders for parentId: " + queryParentId);
        Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            try {
                FileList result = driveService.files().list()
                        .setQ("mimeType = 'application/vnd.google-apps.folder' and '" + queryParentId + "' in parents")
                        .setFields("files(id, name, modifiedTime)")
                        .execute();

                Log.d(TAG, "Folders loaded: " + (result.getFiles() != null ? result.getFiles().size() : 0));
                return result.getFiles();
            } catch (UserRecoverableAuthIOException e) {
                Log.e(TAG, "Authorization required to load folders", e);
                activity.runOnUiThread(() -> authListener.onAuthorizationRequired(e.getIntent()));
                throw e;
            } catch (Exception e) {
                Log.e(TAG, "Error loading folders", e);
                throw e;
            }
        }).addOnSuccessListener(files -> {
            List<FolderItem> folders = new ArrayList<>();
            for (File file : files) {
                folders.add(new FolderItem(
                        file.getName(),
                        file.getId(),
                        getItemCount(file.getId()),
                        formatDate(file.getModifiedTime())
                ));
            }
            folderAdapter.updateFolders(folders);
        }).addOnFailureListener(e -> {
            if (!(e instanceof UserRecoverableAuthIOException)) {
                Toast.makeText(activity, "Failed to load folders: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Загрузка файлов внутри папки folderId
    public void loadFilesInFolder(String folderId) {
        String queryParentId = (folderId != null && !folderId.isEmpty()) ? folderId : "root";

        Log.d(TAG, "Loading files for folderId: " + queryParentId);
        Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            try {
                FileList result = driveService.files().list()
                        .setQ("'" + queryParentId + "' in parents")
                        .setFields("files(id, name, mimeType, modifiedTime)")
                        .execute();

                Log.d(TAG, "Files loaded: " + (result.getFiles() != null ? result.getFiles().size() : 0));
                return result.getFiles();
            } catch (UserRecoverableAuthIOException e) {
                Log.e(TAG, "Authorization required to load files", e);
                activity.runOnUiThread(() -> authListener.onAuthorizationRequired(e.getIntent()));
                throw e;
            } catch (Exception e) {
                Log.e(TAG, "Error loading files", e);
                throw e;
            }
        }).addOnSuccessListener(files -> {
            List<FileItem> fileItems = new ArrayList<>();
            for (File file : files) {
                fileItems.add(new FileItem(
                        file.getName(),
                        file.getId(),
                        file.getMimeType(),
                        formatDate(file.getModifiedTime())
                ));
            }
            fileAdapter.updateFiles(fileItems);
        }).addOnFailureListener(e -> {
            if (!(e instanceof UserRecoverableAuthIOException)) {
                Toast.makeText(activity, "Failed to load files: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Подсчет количества элементов в папке
    private int getItemCount(String folderId) {
        try {
            FileList result = driveService.files().list()
                    .setQ("'" + folderId + "' in parents")
                    .setFields("files(id)")
                    .execute();
            return result.getFiles().size();
        } catch (Exception e) {
            Log.e(TAG, "Error getting item count for folderId: " + folderId, e);
            return 0;
        }
    }

    // Форматирование даты
    private String formatDate(com.google.api.client.util.DateTime dateTime) {
        if (dateTime == null) return "N/A";
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date(dateTime.getValue()));
    }

    public Task<Void> deleteFolder(String folderId) {
        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            try {
                driveService.files().delete(folderId).execute();
                return null;
            } catch (UserRecoverableAuthIOException e) {
                activity.runOnUiThread(() -> authListener.onAuthorizationRequired(e.getIntent()));
                throw e;
            } catch (Exception e) {
                throw e;
            }
        });
    }

}
