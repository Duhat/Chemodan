package com.example.chemodan;

public class FileItem {
    private String id;
    private String name;
    private String localPath;
    private boolean isLocal;
    private String mimeType;
    private String modifiedTime;


    public FileItem(String name, String id, String mimeType, String modifiedTime) {
        this.name = name;
        this.id = id;
        this.mimeType = mimeType;
        this.modifiedTime = modifiedTime;
        this.isLocal = false;
    }

    // Новый конструктор с параметром isLocal
    public FileItem(String name, String id, String mimeType, String modifiedTime, boolean isLocal) {
        this.name = name;
        this.id = id;
        this.mimeType = mimeType;
        this.modifiedTime = modifiedTime;
        this.isLocal = isLocal;
    }

    // Геттеры
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocalPath() {
        return localPath;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getModifiedTime() {
        return modifiedTime;
    }

    // Сеттеры (если нужно изменять поля после создания объекта)
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setModifiedTime(String modifiedTime) {
        this.modifiedTime = modifiedTime;
    }
}
