package com.example.chemodan;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(FileItem file);
        void onFileDelete(FileItem file);
        void onFolderClick(FileItem file); // Добавлен новый метод
    }

    public List<FileItem> getFiles() {
        return files;
    }

    public void setFiles(List<FileItem> files) {
        this.files = files;
        notifyDataSetChanged();
    }
    private List<FileItem> files;
    private final OnFileClickListener listener;
    private final Context context; // Добавлен контекст

    public FileAdapter(Context context, List<FileItem> files, OnFileClickListener listener) {
        this.context = context;
        this.files = files != null ? files : new ArrayList<>();
        this.listener = listener;
    }

    public void updateFiles(List<FileItem> newFiles) {
        this.files = newFiles != null ? newFiles : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem file = files.get(position);
        holder.bind(file, listener);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        private final TextView fileName;
        private final TextView fileDate;
        private final ImageView fileIcon;
        private final Context context; // Контекст для ViewHolder

        public FileViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            this.context = context;
            fileName = itemView.findViewById(R.id.file_name);
            fileDate = itemView.findViewById(R.id.file_date);
            fileIcon = itemView.findViewById(R.id.file_icon);
        }

        public void bind(FileItem file, OnFileClickListener listener) {
            fileName.setText(file.getName());
            fileDate.setText(file.getModifiedTime());

            // Упрощенная установка иконки (можно доработать)
            fileIcon.setImageResource(R.drawable.ic_docs);

            itemView.setOnClickListener(v -> {
                if (file.getMimeType().contains("folder")) {
                    listener.onFolderClick(file);
                } else {
                    listener.onFileClick(file);
                    // Или открывать файл здесь:
                    // openFileInExternalApp(file.getId(), file.getMimeType());
                }
            });

            itemView.setOnLongClickListener(v -> {
                listener.onFileDelete(file);
                return true;
            });
        }

        private void openFileInExternalApp(String fileId, String mimeType) {
            String url = "https://drive.google.com/file/d/" + fileId + "/view";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), mimeType);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show();
            }
        }
    }
}