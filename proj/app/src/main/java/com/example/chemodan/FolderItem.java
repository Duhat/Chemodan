package com.example.chemodan;

public class FolderItem {
    private String name;
    private String path; // id папки
    private int count;   // количество элементов в папке
    private String date; // дата изменения или создания

    public FolderItem(String name, String path, int count, String date) {
        this.name = name;
        this.path = path;
        this.count = count;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public int getCount() {
        return count;
    }

    public String getDate() {
        return date;
    }
}
