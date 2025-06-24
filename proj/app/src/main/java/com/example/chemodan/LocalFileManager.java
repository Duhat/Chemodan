package com.example.chemodan;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LocalFileManager {
    private static final String APP_DIRECTORY = "ChemodanFiles";
    private Context context;

    public LocalFileManager(Context context) {
        this.context = context;
    }

    public boolean saveFile(InputStream inputStream, String fileName, String mimeType, String folderId) {
        File directory = getFolderDirectory(folderId);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            return true;
        } catch (IOException e) {
            Log.e("LocalFileManager", "Error saving file", e);
            return false;
        }
    }

    public List<FileItem> getFilesForFolder(String folderId) {
        List<FileItem> files = new ArrayList<>();
        File directory = getFolderDirectory(folderId);

        if (directory.exists() && directory.isDirectory()) {
            File[] fileList = directory.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    files.add(new FileItem(
                            file.getName(),
                            file.getAbsolutePath(),
                            getMimeType(file.getName()),
                            String.valueOf(file.lastModified()),
                            true // isLocal
                    ));
                }
            }
        }
        return files;
    }

    public boolean deleteFile(FileItem file) {
        if (file.isLocal()) {
            File localFile = new File(file.getLocalPath());
            return localFile.delete();
        }
        return false;
    }

    private File getFolderDirectory(String folderId) {
        File baseDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), APP_DIRECTORY);
        if (folderId == null || folderId.isEmpty()) {
            return baseDir;
        }
        return new File(baseDir, folderId);
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
            return "image/*";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
            return "application/msword";
        } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
            return "application/vnd.ms-excel";
        }
        return "*/*";
    }
}