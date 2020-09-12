package com.example.classify_video.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.classify_video.Classifier.Classifier;
import com.example.classify_video.ExtractVideo.FrameExtraction;
import com.example.classify_video.R;
import com.example.classify_video.Util.MapUtil;

import org.tensorflow.lite.support.common.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * chọn video từ thư mục điện thoại
 * tách video ra frame
 * phân loại video
 */
public class ClassifyVideoActivity extends AppCompatActivity {
    private static final String TAG = "Classify";
    private static final int REQUEST_PERMISSION_RESULT = 0;
    int REQUEST_CODE_FOLDER = 456;
    boolean permission = false;
    public static Uri videoPath;
    VideoView videoView;
    TextView tv_label;
    ImageView imgv_show, imgv_select, imgv_classify;
    int position = 0;
    MediaController mediaController;

    List<Bitmap> listBitmap;
    Classifier classifier;
    List<String> labels;
    Map<String, Float> map;
    String path = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classify_video);
        videoView = findViewById(R.id.video_view);
        tv_label = findViewById(R.id.tv_label);
        imgv_select = findViewById(R.id.imgv_select);
        imgv_classify = findViewById(R.id.imgv_classify);
        imgv_show = findViewById(R.id.imgv_show);
        listBitmap = new ArrayList<>();
        map = new HashMap<>();
        try {
            labels = FileUtil.loadLabels(this, "labels.txt");
            Log.d(TAG, "onCreate: " + labels.size());
            for (String str : labels) map.put(str, (float) 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (this.mediaController == null) {
            this.mediaController = new MediaController(ClassifyVideoActivity.this);
            this.mediaController.setAnchorView(videoView);
            this.videoView.setMediaController(mediaController);
        }
        this.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mediaPlayer) {
                videoView.seekTo(position);
                if (position == 0) {
                    videoView.start();
                }
            }
        });
        //mặc định chạy video trong raw khi bắt đầu app
        path = "android.resource://" + getPackageName() + "/" + R.raw.tennis;
        videoView.setVideoURI(Uri.parse(path));
        //chọn video từ điện thoại
        imgv_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_PICK);
                intent.setType("video/mp4");
                startActivityForResult(intent, REQUEST_CODE_FOLDER);
                tv_label.setText("");
                imgv_show.setImageBitmap(null);
            }
        });
        //click nút ở giữa để phân loại
        imgv_classify.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                ExtractVideo();
                if(listBitmap.size()>0){
                    for (Bitmap bitmap : listBitmap) {
                        Classify(bitmap);
                    }
                    //sắp xếp các giá trị dự đoán
                    map = MapUtil.sortByValue(map);
                    map = MapUtil.div_Map(map, listBitmap.size());
                    Log.d(TAG, "final_result: "+map.toString());
                    //
                    List<Classifier.Recognition> recognitionList = Classifier.getTopKProbability(map, 3);
                    String final_result = "";
                    for (Classifier.Recognition recognition : recognitionList) {
                        final_result += recognition.getTitle() + " : " + String.format("(%.1f%%) ", recognition.getConfidence()) + "\n";
                    }
                    tv_label.setText(final_result);
                    listBitmap.clear();
                    map = MapUtil.resetMap(map);
                }

            }
        });
    }

    private void Classify(final Bitmap bitmap) {
        if (classifier == null) {
            recreateClassifier();
        }

        new Runnable() {

            @Override
            public void run() {
                if (classifier != null) {
                    final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap, 0);
                    Log.d(TAG, "run: " + results);
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.N)
                        @Override
                        public void run() {
                            Classifier.Recognition recognition = results.get(0);
                            String classes = "";
                            float prob = 0f;
                            //tăng % của các class sau mỗi frame
                            for (Classifier.Recognition rec : results) {
                                classes = rec.getTitle();
                                prob = map.get(classes);
                                map.replace(classes, prob, prob + rec.getConfidence() * 100);
                            }
                        }
                    });
                }
            }
        }.run();
    }

    private void recreateClassifier() {
        if (classifier != null) {
            classifier.close();
            classifier = null;
        }
        try {
            classifier = Classifier.create(this);
        } catch (IOException e) {
            Log.e("RecreateClassifier Fail", "Failed to create classifier: " + e);
        }
    }

    private void ExtractVideo() {
        if(videoPath == null) Toast.makeText(this, "Chọn video để thực hiện phân loại", Toast.LENGTH_SHORT).show();
        else{
            FrameExtraction extraction = new FrameExtraction(videoPath,this);
            listBitmap = extraction.getListFrame();
            Log.d(TAG, "list frame size: " + listBitmap.size());
            imgv_show.setImageBitmap(listBitmap.get(0));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_FOLDER) {
                videoPath = data.getData();
                Log.d("TAG", "video path: " + videoPath);
                if (videoPath != null) {
                    videoView.setVideoURI(videoPath);
                    videoView.start();
                }
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (permission == false) grantPermission();
    }

    private void grantPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!cameraGranted() || !externalStorageGranted()) {
                requestPermissions(new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                }, REQUEST_PERMISSION_RESULT);
            }
        }
    }

    private boolean cameraGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean externalStorageGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_RESULT && grantResults.length == 3) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "The application will not run without camera services!", Toast.LENGTH_SHORT).show();
            }
            if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "The application will not run without read storage services!", Toast.LENGTH_SHORT).show();
            }
            if (grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "The application will not run without write storage services!", Toast.LENGTH_SHORT).show();
            }
            if (grantResults[0] == grantResults[1] && grantResults[1] == grantResults[2] && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permission = true;
            }
        }
    }
}