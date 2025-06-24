package com.example.chemodan;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.ArrayList;
import java.util.List;

public class doc_main_activity extends AppCompatActivity implements DriveManager.DriveAuthListener {

    private RecyclerView recyclerView;
    private FolderAdapter adapter;
    private DriveManager driveManager;
    private String currentFolderId = null; // если нужно открывать подпапки
    private final Handler handler = new Handler();
    private static final int REQUEST_AUTHORIZATION = 1001;
    private static final long REFRESH_INTERVAL_MS = 5000; // каждые 5 секунд обновлять

    // Метод, вызываемый при создании Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc_main);

        // Инициализация меню (сбоку)
        androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.doc_drawer_layout);
        NavigationMenu navigationMenu = new NavigationMenu(this, drawerLayout);
        ImageView btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> navigationMenu.openDrawer());

        // Инициализация RecyclerView (список папок)
        recyclerView = findViewById(R.id.recycler_folders);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Адаптер для отображения папок
        adapter = new FolderAdapter(new ArrayList<>(), new FolderAdapter.OnFolderClickListener() {
            @Override
            public void onFolderClick(FolderItem folder) {
                // Открыть содержимое папки
                Intent intent = new Intent(doc_main_activity.this, FolderContentActivity.class);
                intent.putExtra("folder_id", folder.getPath());
                intent.putExtra("folder_name", folder.getName());
                startActivity(intent);
            }

            @Override
            public void onFolderMenuClick(FolderItem folder, View anchor) {
                // Показать меню с опциями
                showFolderMenu(folder, anchor);
            }
        });
        recyclerView.setAdapter(adapter);

        // Добавление свайпа для удаления
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                FolderItem folder = adapter.getFolderAtPosition(position);

                // Подтверждение удаления
                new AlertDialog.Builder(doc_main_activity.this)
                        .setTitle("Удалить папку")
                        .setMessage("Вы уверены, что хотите удалить \"" + folder.getName() + "\"?")
                        .setPositiveButton("Удалить", (dialog, which) -> {
                            driveManager.deleteFolder(folder.getPath())
                                    .addOnSuccessListener(aVoid -> {
                                        adapter.removeFolderAtPosition(position);
                                        Toast.makeText(doc_main_activity.this, "Папка удалена", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(doc_main_activity.this, "Ошибка удаления папки", Toast.LENGTH_SHORT).show();
                                        adapter.notifyItemChanged(position); // Вернуть обратно
                                    });
                        })
                        .setNegativeButton("Отмена", (dialog, which) -> {
                            adapter.notifyItemChanged(position); // Вернуть обратно
                            dialog.dismiss();
                        })
                        .setCancelable(false)
                        .show();
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);

        // Проверка авторизации
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            driveManager = DriveManager.getInstance(this, account, this);
        }

        // Обработка кнопки "Создать папку"
        findViewById(R.id.fab_add_folder).setOnClickListener(v -> showCreateFolderDialog());

        // Загрузка папок
        loadAndDisplayFolders();
    }

    // Загрузка списка папок и отображение их в адаптере
    private void loadAndDisplayFolders() {
        String parentId = (currentFolderId != null) ? currentFolderId : null;

        driveManager.loadFolders(parentId)
                .addOnSuccessListener(files -> {
                    List<FolderItem> folderItems = new ArrayList<>();

                    for (com.google.api.services.drive.model.File file : files) {
                        String name = file.getName();
                        String id = file.getId();
                        String date = file.getModifiedTime() != null ? file.getModifiedTime().toStringRfc3339() : "неизвестно";
                        int count = 0; // Тут пока заглушка, можно позже считать содержимое

                        folderItems.add(new FolderItem(name, id, count, date));
                    }

                    adapter.updateFolders(folderItems);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Не удалось загрузить папки", Toast.LENGTH_SHORT).show();
                });
    }

    // Диалог для создания новой папки
    private void showCreateFolderDialog() {
        EditText input = new EditText(this);
        input.setHint("Имя папки");

        new AlertDialog.Builder(this)
                .setTitle("Создать папку")
                .setView(input)
                .setPositiveButton("Создать", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        driveManager.createFolder(name, currentFolderId)
                                .addOnSuccessListener(id -> {
                                    Toast.makeText(this, "Папка создана", Toast.LENGTH_SHORT).show();
                                    loadAndDisplayFolders();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Не удалось создать папку", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Имя не может быть пустым", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // Отображение всплывающего меню для папки
    private void showFolderMenu(FolderItem folder, View anchor) {
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

    // Удаление выбранной папки
    private void deleteFolder(FolderItem folder) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить папку")
                .setMessage("Вы уверены, что хотите удалить \"" + folder.getName() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    driveManager.deleteFolder(folder.getPath())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Папка удалена", Toast.LENGTH_SHORT).show();
                                loadAndDisplayFolders();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Ошибка при удалении папки", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // Обновление списка каждые 5 секунд
    private final Runnable refreshFoldersRunnable = new Runnable() {
        @Override
        public void run() {
            loadAndDisplayFolders();
            handler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

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

    // Обработка запроса авторизации
    @Override
    public void onAuthorizationRequired(Intent authorizationIntent) {
        startActivityForResult(authorizationIntent, REQUEST_AUTHORIZATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_AUTHORIZATION && resultCode == RESULT_OK) {
            driveManager.createRootFolderIfNeeded();
        }
    }
}
