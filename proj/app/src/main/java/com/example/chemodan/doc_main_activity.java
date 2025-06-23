package com.example.chemodan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.ArrayList;
import java.util.Collections;

public class doc_main_activity extends AppCompatActivity implements DriveManager.DriveAuthListener {

    private static final int REQUEST_CODE_SIGN_IN = 100;
    private static final int REQUEST_AUTHORIZATION = 101;

    private DrawerLayout drawerLayout;
    private DriveManager driveManager;
    private FolderAdapter adapter;
    private String currentFolderId = null;

    private final Handler handler = new Handler();
    private final int REFRESH_INTERVAL_MS = 10_000; // 10 секунд

    private final Runnable refreshFoldersRunnable = new Runnable() {
        @Override
        public void run() {
            if (driveManager != null && currentFolderId != null) {
                driveManager.loadFolders(currentFolderId);
            }
            handler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc_main);

        drawerLayout = findViewById(R.id.doc_drawer_layout);
        NavigationMenu navigationMenu = new NavigationMenu(this, drawerLayout);

        ImageView btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> navigationMenu.openDrawer());

        RecyclerView recyclerView = findViewById(R.id.recycler_folders);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new FolderAdapter(new ArrayList<>(), new FolderAdapter.OnFolderClickListener() {
            @Override
            public void onFolderClick(FolderItem folder) {
                Intent intent = new Intent(doc_main_activity.this, FolderContentActivity.class);
                intent.putExtra("folder_id", folder.getPath());
                intent.putExtra("folder_name", folder.getName());
                startActivity(intent);
            }

            @Override
            public void onFolderMenuClick(FolderItem folder, android.view.View anchor) {
                showFolderMenu(folder, anchor);
            }
        });
        recyclerView.setAdapter(adapter);

        // Поддержка свайпа для удаления с подтверждением
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false; // Перемещение не нужно
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                FolderItem folderToDelete = adapter.getFolderAtPosition(position);

                new AlertDialog.Builder(doc_main_activity.this)
                        .setTitle("Delete Folder")
                        .setMessage("Are you sure you want to delete folder \"" + folderToDelete.getName() + "\"?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            driveManager.deleteFolder(folderToDelete.getPath())
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(doc_main_activity.this, "Folder deleted", Toast.LENGTH_SHORT).show();
                                        adapter.removeFolderAtPosition(position);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(doc_main_activity.this, "Failed to delete folder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        adapter.notifyItemChanged(position);
                                    });
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            adapter.notifyItemChanged(position);
                            dialog.dismiss();
                        })
                        .setCancelable(false)
                        .show();
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            signIn();
            return;
        }

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

        driveManager = new DriveManager(this, driveService, adapter, null, this);

        // Запускаем создание корневой папки и загрузку папок
        driveManager.createRootFolderIfNeeded();

        findViewById(R.id.fab_add_folder).setOnClickListener(v -> showCreateFolderDialog());

        setupSearch();
    }

    private void signIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .requestEmail()
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this, gso);
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private void showCreateFolderDialog() {
        EditText input = new EditText(this);
        input.setHint("Folder name");

        new AlertDialog.Builder(this)
                .setTitle("New Folder")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        String parentId = (currentFolderId != null && !currentFolderId.isEmpty()) ? currentFolderId : null;
                        driveManager.createFolder(name, parentId)
                                .addOnSuccessListener(id -> driveManager.loadFolders(parentId != null ? parentId : ""))
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to create folder: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFolderMenu(FolderItem folder, android.view.View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.folder_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_delete) {
                deleteFolder(folder);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void deleteFolder(FolderItem folder) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Folder")
                .setMessage("Are you sure you want to delete " + folder.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    driveManager.deleteFolder(folder.getPath())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Folder deleted", Toast.LENGTH_SHORT).show();
                                driveManager.loadFolders(currentFolderId != null ? currentFolderId : "");
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete folder: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupSearch() {
        EditText search = findViewById(R.id.search_doc);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // TODO: Реализовать фильтрацию списка папок по названию
            }
        });
    }

    @Override
    public void onAuthorizationRequired(Intent authorizationIntent) {
        startActivityForResult(authorizationIntent, REQUEST_AUTHORIZATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                recreate();
            } else {
                Toast.makeText(this, "Sign-in failed", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_AUTHORIZATION) {
            if (resultCode == RESULT_OK) {
                driveManager.createRootFolderIfNeeded();
            } else {
                Toast.makeText(this, "Authorization required to access Drive", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshFoldersRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshFoldersRunnable);
    }
}
