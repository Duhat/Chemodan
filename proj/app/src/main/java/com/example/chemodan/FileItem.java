package com.example.chemodan;

public class FileItem {
    private String name;
    private String id;
    private String mimeType;
    private String modifiedTime;

    public FileItem(String name, String id, String mimeType, String modifiedTime) {
        this.name = name;
        this.id = id;
        this.mimeType = mimeType;
        this.modifiedTime = modifiedTime;
    }

    public String getName() { return name; }
    public String getId() { return id; }
    public String getMimeType() { return mimeType; }
    public String getModifiedTime() { return modifiedTime; }
}
