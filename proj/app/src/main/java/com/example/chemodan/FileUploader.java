package com.example.chemodan;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import com.google.api.services.drive.Drive;

import java.io.InputStream;
import java.io.IOException;

public class FileUploader {
    private static final String TAG = "FileUploader";

    public static void uploadFile(Context context, Uri fileUri, String folderId,
                                  Drive driveService, DriveManager.DriveAuthListener listener) {
        new Thread(() -> {
            try {
                String fileName = getFileName(context, fileUri);
                // Get an InputStream from the Uri
                try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri)) {
                    if (inputStream != null) {
                        String mimeType = context.getContentResolver().getType(fileUri);
                        DriveManager.getInstance(context, driveService, listener)
                                .uploadFile(inputStream, fileName, mimeType, folderId)
                                .addOnSuccessListener(id -> {
                                    Log.d(TAG, "File uploaded: " + id);
                                    Toast.makeText(context, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Upload failed", e);
                                    Toast.makeText(context, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        throw new IOException("Could not open input stream for the file");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error preparing upload", e);
                Toast.makeText(context, "Error preparing upload: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }).start();
    }

    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name", e);
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
}