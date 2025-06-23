package com.example.chemodan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.ArrayList;
import java.util.Collections;

public class FolderContentActivity extends AppCompatActivity implements DriveManager.DriveAuthListener {

    private DriveManager driveManager;
    private FileAdapter fileAdapter;
    private String currentFolderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_content);

        currentFolderId = getIntent().getStringExtra("folder_id");
        String folderName = getIntent().getStringExtra("folder_name");

        setTitle(folderName);

        // Инициализация RecyclerView для файлов
        RecyclerView recyclerView = findViewById(R.id.recycler_files);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fileAdapter = new FileAdapter(new ArrayList<>(), new FileAdapter.OnFileClickListener() {
            @Override
            public void onFileClick(FileItem file) {
                // Обработка клика по файлу
                Toast.makeText(FolderContentActivity.this,
                        "Clicked: " + file.getName(), Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(fileAdapter);

        // Инициализация DriveManager
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    this,
                    Collections.singleton(DriveScopes.DRIVE_FILE)
            );
            credential.setSelectedAccount(account.getAccount());

            Drive driveService = new Drive.Builder(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    credential)
                    .setApplicationName("Doc Storage")
                    .build();

            driveManager = new DriveManager(this, driveService, null, fileAdapter, this);
            driveManager.loadFilesInFolder(currentFolderId);
        }
    }

    @Override
    public void onAuthorizationRequired(Intent authorizationIntent) {
        startActivityForResult(authorizationIntent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            driveManager.loadFilesInFolder(currentFolderId);
        }
    }
}