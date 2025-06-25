package com.example.chemodan;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChemodanActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST_CODE = 1001;
    private static final String TAG = "ChemodanActivity";

    private FloatingActionButton fabUpload;
    private RecyclerView recyclerView;
    private FileListAdapter adapter;
    private File chemodanDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chemodan);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fabUpload = findViewById(R.id.fab_upload_file);
        recyclerView = findViewById(R.id.recycler_files);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FileListAdapter(new FileListAdapter.OnFileClickListener() {
            @Override
            public void onFileClick(File file) {
                openFile(file);
            }

            @Override
            public void onFileLongClick(File file, int position) {
                Log.d(TAG, "Long click on file: " + file.getName());
                confirmDeleteFile(file, position);
            }
        });
        recyclerView.setAdapter(adapter);

        fabUpload.setOnClickListener(v -> openFilePicker());

        chemodanDir = new File(getFilesDir(), "Chemodan");
        if (!chemodanDir.exists()) chemodanDir.mkdirs();

        loadSavedFiles();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
        startActivityForResult(Intent.createChooser(intent, "Выберите файл"), PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            saveFileToInternalStorage(data.getData());
        }
    }

    private void saveFileToInternalStorage(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            String fileName = "file_" + System.currentTimeMillis();
            String extension = getContentResolver().getType(fileUri).contains("pdf") ? ".pdf" : ".jpg";

            File file = new File(chemodanDir, fileName + extension);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.close();
            inputStream.close();

            Toast.makeText(this, "Файл сохранён", Toast.LENGTH_SHORT).show();
            loadSavedFiles();
        } catch (IOException e) {
            Log.e(TAG, "Ошибка сохранения", e);
            Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadSavedFiles() {
        if (chemodanDir.exists()) {
            File[] filesArray = chemodanDir.listFiles();
            List<File> files = filesArray != null ? Arrays.asList(filesArray) : new ArrayList<>();
            adapter.updateFiles(files);
        }
    }

    private void openFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        String mimeType = getContentResolver().getType(fileUri);
        if (mimeType == null) mimeType = "*/*";

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Нет приложения для открытия файла", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteFile(File file, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить файл")
                .setMessage("Вы уверены, что хотите удалить файл \"" + file.getName() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    if (file.delete()) {
                        loadSavedFiles(); // Обновляем список после удаления
                        Toast.makeText(this, "Файл удалён", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Не удалось удалить файл", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}
