package com.example.classify_video.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.example.classify_video.Database.ImageAdapter;
import com.example.classify_video.Database.MyDatabase;
import com.example.classify_video.Database.MyImage;
import com.example.classify_video.Database.MyVideo;
import com.example.classify_video.Database.VideoAdapter;
import com.example.classify_video.R;
import com.example.classify_video.Util.BitmapUtil;

import java.io.File;
import java.util.ArrayList;

public class ListImageActivity extends AppCompatActivity implements ImageAdapter.itemClickIntf {
    private static final String TAG = "ListImageActivity";
    MyDatabase database;
    RecyclerView recyclerView;
    ImageAdapter adapter;
    ImageView imageView;
    ArrayList<MyImage> image_recommend;
    RecyclerView.LayoutManager layoutManager;
    Bitmap bitmap;
    int allImage = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_image);
        database = new MyDatabase(this);
        recyclerView = findViewById(R.id.img_recyclerview);
        imageView = findViewById(R.id.img_viewinlist);
        image_recommend = new ArrayList<>();
        layoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        Intent intent = getIntent();
        allImage = intent.getIntExtra("All_image",0);
        if(allImage!=0){
            showAllImage();
        }else{
            String title = intent.getStringExtra("image_label");
            String imagePath = intent.getStringExtra("image_path");
            Log.d(TAG, "receive fom picture: "+title+"-"+imagePath);
            if(imagePath!=null){
//            Uri uri = Uri.fromFile(new File(imagePath));
//            Log.d(TAG, "conver uri form path: "+uri);
//            bitmap = BitmapUtil.getBitmapFromGallery(this, uri);
                bitmap = BitmapFactory.decodeFile(imagePath);
                Log.d(TAG, "onCreate: "+bitmap);
                imageView.setImageBitmap(bitmap);
            }
            if(title!=null){
                showRecommend(title);
            }
        }

    }

    private void showAllImage() {
        ArrayList<MyImage> listImage = new ArrayList<>();
        listImage = database.getAllImage();
        bitmap = BitmapFactory.decodeFile(listImage.get(0).getImage_path());
        imageView.setImageBitmap(bitmap);
        adapter = new ImageAdapter(this, listImage);
        recyclerView.setAdapter(adapter);
    }

    private void showRecommend(String title1) {
        Log.d(TAG, "showRecommend chủ đề: " + title1);
//        ArrayList<MyVideo> video_recommend = new ArrayList<>();
        image_recommend = database.getImageByType(title1);
        Log.d(TAG, "showRecommend: " + image_recommend.size());
        for (MyImage v : image_recommend) {
            Log.d(TAG, "showRecommend: " + v.getImage_path());
        }
        adapter = new ImageAdapter(this, image_recommend);
        recyclerView.setAdapter(adapter);
    }
    @Override
    public void itemClicked(int position, ArrayList<MyImage> myImages) {
        bitmap = BitmapFactory.decodeFile(myImages.get(position).getImage_path());
        imageView.setImageBitmap(bitmap);
    }
}