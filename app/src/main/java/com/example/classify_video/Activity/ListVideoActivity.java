package com.example.classify_video.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.classify_video.Database.MyDatabase;
import com.example.classify_video.Database.MyVideo;
import com.example.classify_video.Database.VideoAdapter;
import com.example.classify_video.R;

import java.util.ArrayList;

public class ListVideoActivity extends AppCompatActivity implements VideoAdapter.itemClickIntf {
    MyDatabase database;
    RecyclerView recyclerView;
    VideoAdapter adapter;
    VideoView videoView;
    ArrayList<MyVideo> video_recommend;
    MediaController mediaController;
    RecyclerView.LayoutManager layoutManager;
    private int position = 0;
    private static final String TAG = "ListVideoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_video);
        recyclerView = findViewById(R.id.recyclerview);
        videoView = findViewById(R.id.video_viewinlist);
        database = new MyDatabase(this);
        video_recommend = new ArrayList<>();
        layoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        Intent intent = getIntent();
        String title = intent.getStringExtra("video_label");
        String videopath = intent.getStringExtra("video_path");
        if (videopath == null || title == null) {
            showAllVideo();
        } else {
            videoView.setVideoURI(Uri.parse(videopath));
            videoView.start();
//        if (videopath != null) {
//            videoView.setVideoURI(Uri.parse(videopath));
//            videoView.start();
//            if (this.mediaController == null) {
//                this.mediaController = new MediaController(ListVideoActivity.this);
//                this.mediaController.setAnchorView(videoView);
//                this.videoView.setMediaController(mediaController);
//            }
//            this.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                public void onPrepared(MediaPlayer mediaPlayer) {
//                    videoView.seekTo(position);
//                    if (position == 0) {
//                        videoView.start();
//                    }
//                }
//            });
//        }
//        if (title != null) {
            showRecommend(title);
        }
    }

    private void showAllVideo() {
        ArrayList<MyVideo> video_recommend1 = new ArrayList<>();
        video_recommend1 = database.getAllVideo();
        adapter = new VideoAdapter(this, video_recommend1);
        recyclerView.setAdapter(adapter);
        videoView.setVideoURI(Uri.parse(video_recommend1.get(0).getVideo_path()));
        videoView.start();
    }

    private void showRecommend(String title1) {
        Log.d(TAG, "showRecommend chủ đề: " + title1);
//        ArrayList<MyVideo> video_recommend = new ArrayList<>();
        video_recommend = database.getVideoByType(title1);
        Log.d(TAG, "showRecommend: " + video_recommend.size());
        for (MyVideo v : video_recommend) {
            Log.d(TAG, "showRecommend: " + v.getVideo_path());
        }
        adapter = new VideoAdapter(this, video_recommend);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void itemClicked(int position, ArrayList<MyVideo> myvideos) {
        Uri uri = Uri.parse(myvideos.get(position).getVideo_path());
        Log.d(TAG, "itemClicked: " + (uri == null));
        videoView.setVideoURI(uri);
        videoView.start();
    }
}