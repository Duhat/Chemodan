package com.example.chemodan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(FileItem file);
    }

    private List<FileItem> files;
    private final OnFileClickListener listener;

    public FileAdapter(List<FileItem> files, OnFileClickListener listener) {
        this.files = files;
        this.listener = listener;
    }

    public void updateFiles(List<FileItem> newFiles) {
        this.files = newFiles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem file = files.get(position);
        holder.bind(file, listener);
    }

    @Override
    public int getItemCount() {
        return files != null ? files.size() : 0;
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        private final TextView fileName;
        private final TextView fileDate;
        private final ImageView fileIcon;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.file_name);
            fileDate = itemView.findViewById(R.id.file_date);
            fileIcon = itemView.findViewById(R.id.file_icon);
        }

        public void bind(FileItem file, OnFileClickListener listener) {
            fileName.setText(file.getName());
            fileDate.setText(file.getModifiedTime());

            // Установка иконки в зависимости от типа файла
            if (file.getMimeType().contains("folder")) {
                fileIcon.setImageResource(R.drawable.ic_folder);
            } else if (file.getMimeType().contains("image")) {
                fileIcon.setImageResource(R.drawable.ic_docs);
            } else {
                fileIcon.setImageResource(R.drawable.ic_docs);
            }

            itemView.setOnClickListener(v -> listener.onFileClick(file));
        }
    }
}