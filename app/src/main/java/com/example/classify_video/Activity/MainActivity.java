package com.example.classify_video.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.classify_video.Classifier.Classifier;
import com.example.classify_video.R;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSION_RESULT = 0;
    private static final int REQUEST_CODE_FOLDER = 111;
    private static final String TAG = "MainActivity";
//    private Classifier.Device device = Classifier.Device.CPU;
//    private Classifier.Model model = Classifier.Model.FLOAT;
    private int numThreads = 3;
    TextView tv_result;
    ImageView imageView;
    ImageButton ibtn_select, ibtn_capture;
    boolean permission = false;
    Classifier classifier;
    Bitmap rgbBitmap;
    InputStream inputStream;
    Resources resources;
    List<Bitmap> list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ibtn_capture = findViewById(R.id.imgCapture);
        resources = getResources();
        inputStream = resources.openRawResource(R.raw.video);
        list = new ArrayList<>();
        ibtn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ClassifyVideoActivity.class);
                startActivity(intent);
            }
        });
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