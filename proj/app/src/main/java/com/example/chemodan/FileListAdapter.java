package com.example.chemodan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(File file);
        void onFileLongClick(File file, int position);
    }

    private List<File> files;
    private final OnFileClickListener listener;

    public FileListAdapter(OnFileClickListener listener) {
        this.listener = listener;
    }

    public void updateFiles(List<File> newFiles) {
        this.files = newFiles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileListAdapter.ViewHolder holder, int position) {
        File file = files.get(position);
        holder.textFileName.setText(file.getName());

        if (isImageFile(file)) {
            Bitmap bitmap = decodeSampledBitmapFromFile(file.getAbsolutePath(), 40, 40);
            holder.imagePreview.setImageBitmap(bitmap);
        } else {
            holder.imagePreview.setImageResource(R.drawable.ic_docs);
        }

        holder.itemView.setOnClickListener(v -> listener.onFileClick(file));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onFileLongClick(file, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return files != null ? files.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePreview;
        TextView textFileName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.image_preview);
            textFileName = itemView.findViewById(R.id.text_file_name);
        }
    }

    private boolean isImageFile(File file) {
        String[] imageExtensions = { "jpg", "jpeg", "png", "gif", "bmp" };
        String name = file.getName().toLowerCase();
        for (String ext : imageExtensions) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }

    private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
