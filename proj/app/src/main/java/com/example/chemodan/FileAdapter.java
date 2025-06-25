package com.example.chemodan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(File file);
        void onFileLongClick(File file, int position);
    }

    private final List<File> fileList;
    private final OnFileClickListener listener;

    public FileAdapter(List<File> fileList, OnFileClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
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
        File file = fileList.get(position);
        holder.fileName.setText(file.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileClick(file);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onFileLongClick(file, holder.getAdapterPosition());
            }
            return true; // событие обработано
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.text_file_name);
        }
    }
}
