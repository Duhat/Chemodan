package com.example.chemodan;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FolderContentActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 102;
    private static final int AUTH_REQUEST_CODE = 101;

    private DriveManager driveManager;
    private FileAdapter fileAdapter;
    private String currentFolderId;
    private LocalFileManager localFileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_content);

        currentFolderId = getIntent().getStringExtra("folder_id");
        String folderName = getIntent().getStringExtra("folder_name");
        setTitle(folderName);

        localFileManager = new LocalFileManager(this);

        RecyclerView recyclerView = findViewById(R.id.recycler_files);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fileAdapter = new FileAdapter(this, new ArrayList<>(), new FileAdapter.OnFileClickListener() {
            @Override
            public void onFileClick(FileItem file) {
                handleFileClick(file);
            }

            @Override
            public void onFileDelete(FileItem file) {
                showDeleteConfirmation(file);
            }

            @Override
            public void onFolderClick(FileItem folder) {
                // Handle folder click if needed
            }
        });
        recyclerView.setAdapter(fileAdapter);

        setupSwipeToDelete(recyclerView);

        FloatingActionButton fabAddFile = findViewById(R.id.fab_add_file);
        fabAddFile.setOnClickListener(v -> showFilePicker());

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            driveManager = DriveManager.getInstance(this, account, new DriveManager.DriveAuthListener() {
                @Override
                public void onAuthorizationRequired(Intent authorizationIntent) {
                    startActivityForResult(authorizationIntent, AUTH_REQUEST_CODE);
                }
            });
        }

        loadFiles();
    }

    private void setupSwipeToDelete(RecyclerView recyclerView) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                FileItem file = fileAdapter.getFiles().get(position);
                showDeleteConfirmation(file);
                fileAdapter.notifyItemChanged(position);
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void loadFiles() {
        List<FileItem> localFiles = localFileManager.getFilesForFolder(currentFolderId);
        fileAdapter.updateFiles(localFiles);

        if (driveManager != null) {
            driveManager.listFiles(currentFolderId)
                    .addOnSuccessListener(files -> {
                        List<FileItem> driveFiles = convertDriveFiles(files);
                        List<FileItem> allFiles = new ArrayList<>();
                        allFiles.addAll(localFiles);
                        allFiles.addAll(driveFiles);
                        fileAdapter.updateFiles(allFiles);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error loading Drive files", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private List<FileItem> convertDriveFiles(List<com.google.api.services.drive.model.File> files) {
        List<FileItem> result = new ArrayList<>();
        for (com.google.api.services.drive.model.File file : files) {
            result.add(new FileItem(
                    file.getName(),
                    file.getId(),
                    file.getMimeType(),
                    file.getModifiedTime() != null ? file.getModifiedTime().toString() : "",
                    false
            ));
        }
        return result;
    }

    private void handleFileClick(FileItem file) {
        if (file.isLocal()) {
            openLocalFile(file);
        } else {
            Toast.makeText(this, "Opening Drive file: " + file.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    private void openLocalFile(FileItem file) {
        try {
            Uri fileUri = Uri.fromFile(new File(file.getLocalPath()));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, file.getMimeType());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app available to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation(FileItem file) {
        new AlertDialog.Builder(this)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete " + file.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteFile(file))
                .setNegativeButton("Cancel", (dialog, which) -> fileAdapter.notifyDataSetChanged())
                .show();
    }

    private void deleteFile(FileItem file) {
        if (file.isLocal()) {
            boolean deleted = localFileManager.deleteFile(file);
            if (deleted) {
                Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show();
                loadFiles();
            } else {
                Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
            }
        } else if (driveManager != null) {
            driveManager.deleteFile(file.getId())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show();
                        loadFiles();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                String fileName = getFileName(uri);
                String mimeType = getContentResolver().getType(uri);
                InputStream inputStream = getContentResolver().openInputStream(uri);

                if (inputStream != null) {
                    boolean saved = localFileManager.saveFile(inputStream, fileName, mimeType, currentFolderId);
                    if (saved) {
                        Toast.makeText(this, "File added", Toast.LENGTH_SHORT).show();
                        loadFiles();
                    } else {
                        Toast.makeText(this, "Failed to add file", Toast.LENGTH_SHORT).show();
                    }
                    inputStream.close();
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error adding file", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == AUTH_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                loadFiles();
            } else {
                Toast.makeText(this, "Authorization failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
}