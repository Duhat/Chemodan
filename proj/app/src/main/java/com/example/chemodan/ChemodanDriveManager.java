package com.example.chemodan;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ChemodanDriveManager {

    private static final String ROOT_FOLDER_NAME = "Chemodan";

    private final Context context;

    public ChemodanDriveManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Создает корневую папку "Chemodan" в локальном хранилище приложения, если ее еще нет.
     * @return File объект папки.
     */
    public File createRootFolderIfNeeded() {
        File rootFolder = new File(context.getFilesDir(), ROOT_FOLDER_NAME);
        if (!rootFolder.exists()) {
            boolean created = rootFolder.mkdirs();
            if (!created) {
                Log.e("ChemodanDriveManager", "Не удалось создать папку " + ROOT_FOLDER_NAME);
            }
        }
        return rootFolder;
    }

    /**
     * Сохраняет файл из InputStream в папку "Chemodan" с указанным именем.
     * @param inputStream поток файла для сохранения
     * @param fileName имя файла (например, "photo_12345.jpg")
     * @return File объект сохраненного файла
     * @throws Exception если не удалось сохранить файл
     */
    public File saveFileToLocalFolder(InputStream inputStream, String fileName) throws Exception {
        File folder = createRootFolderIfNeeded();
        File file = new File(folder, fileName);

        try (OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (Exception e) {
            Log.e("ChemodanDriveManager", "Ошибка сохранения файла: " + e.getMessage(), e);
            throw e;
        }

        return file;
    }
}
