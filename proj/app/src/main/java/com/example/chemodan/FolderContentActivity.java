package com.example.chemodan;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderContentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private List<File> fileList;
    private File folder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_content);

        recyclerView = findViewById(R.id.recycler_folder_files);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        folder = (File) getIntent().getSerializableExtra("folder");
        if (folder == null) {
            Toast.makeText(this, "Ошибка: папка не найдена", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fileList = new ArrayList<>();
        fileAdapter = new FileAdapter(fileList, new FileAdapter.OnFileClickListener() {
            @Override
            public void onFileClick(File file) {
                openFile(file);
            }

            @Override
            public void onFileLongClick(File file, int position) {
                Log.d("FolderContentActivity", "Long click on: " + file.getName());
                confirmDeleteFile(file, position);
            }
        });
        recyclerView.setAdapter(fileAdapter);

        loadFiles();
    }

    private void loadFiles() {
        fileList.clear();
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileList.add(file);
                    }
                }
            }
        } else {
            Toast.makeText(this, "Папка не существует или недоступна", Toast.LENGTH_SHORT).show();
        }
        fileAdapter.notifyDataSetChanged();
    }

    private void openFile(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(
                    androidx.core.content.FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".provider",
                            file
                    ),
                    getMimeType(file.getName())
            );
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Открыть с помощью"));
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть файл", Toast.LENGTH_SHORT).show();
            Log.e("FileOpenError", "Error opening file", e);
        }
    }

    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "pdf": return "application/pdf";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "txt": return "text/plain";
            case "doc":
            case "docx": return "application/msword";
            case "xls":
            case "xlsx": return "application/vnd.ms-excel";
            default: return "*/*";
        }
    }

    private void confirmDeleteFile(File file, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить файл")
                .setMessage("Вы уверены, что хотите удалить файл \"" + file.getName() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    if (file.delete()) {
                        fileList.remove(position);
                        fileAdapter.notifyItemRemoved(position);
                        Toast.makeText(this, "Файл удалён", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Не удалось удалить файл", Toast.LENGTH_SHORT).show();
                        fileAdapter.notifyItemChanged(position);
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> {
                    fileAdapter.notifyItemChanged(position);
                })
                .setOnCancelListener(dialog -> {
                    fileAdapter.notifyItemChanged(position);
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFiles();
    }
}
