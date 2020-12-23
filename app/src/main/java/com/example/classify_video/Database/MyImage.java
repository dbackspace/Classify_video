package com.example.classify_video.Database;

public class MyImage {

    private int id;
    private String image_path;
    private String type;

    public MyImage() {
    }

    public MyImage( String image_path, String type) {
        this.image_path = image_path;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getImage_path() {
        return image_path;
    }

    public void setImage_path(String image_path) {
        this.image_path = image_path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
