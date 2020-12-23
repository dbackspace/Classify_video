package com.example.classify_video.Database;

public class MyVideo {
    private int id;
    private String video_path;
    private String type;

    public MyVideo() {
    }

    public MyVideo(String video_path, String type) {
        this.video_path = video_path;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getVideo_path() {
        return video_path;
    }

    public void setVideo_path(String video_path) {
        this.video_path = video_path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
