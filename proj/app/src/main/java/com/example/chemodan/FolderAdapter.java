package com.example.chemodan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    public interface OnFolderClickListener {
        void onFolderClick(FolderItem folder);
        void onFolderMenuClick(FolderItem folder, View anchor);
    }

    private List<FolderItem> folders;
    private final OnFolderClickListener listener;

    public FolderAdapter(List<FolderItem> folders, OnFolderClickListener listener) {
        this.folders = folders;
        this.listener = listener;
    }

    public void updateFolders(List<FolderItem> newFolders) {
        this.folders = newFolders;
        notifyDataSetChanged();
    }

    public FolderItem getFolderAtPosition(int position) {
        return folders.get(position);
    }

    public void removeFolderAtPosition(int position) {
        folders.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        FolderItem folder = folders.get(position);
        holder.bind(folder, listener);
    }

    @Override
    public int getItemCount() {
        return folders != null ? folders.size() : 0;
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView countTextView;
        private final TextView dateTextView;
        private final ImageView menuImageView;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.folder_name);
            countTextView = itemView.findViewById(R.id.folder_count);
            dateTextView = itemView.findViewById(R.id.folder_date);
            menuImageView = itemView.findViewById(R.id.folder_menu);
        }

        public void bind(FolderItem folder, OnFolderClickListener listener) {
            nameTextView.setText(folder.getName());
            countTextView.setText(String.valueOf(folder.getCount()));
            dateTextView.setText(folder.getDate());

            itemView.setOnClickListener(v -> listener.onFolderClick(folder));

            menuImageView.setOnClickListener(v -> listener.onFolderMenuClick(folder, menuImageView));
        }
    }
}
