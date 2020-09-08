package com.example.classify_video;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.classify_video.Classifier.Classifier;
import com.example.classify_video.Classifier.VideoClassifier;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        tv_result = findViewById(R.id.tv_result);
        imageView = findViewById(R.id.imgv);
        ibtn_select = findViewById(R.id.imgPreview);
        ibtn_capture = findViewById(R.id.imgCapture);
        ibtn_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CODE_FOLDER);
            }
        });
        ibtn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBitmap();
                try {
                    classifyImage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getBitmap() {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
        rgbBitmap = bitmapDrawable.getBitmap();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FOLDER && resultCode == Activity.RESULT_OK && data != null) {
            Toast.makeText(this, "Returned image from storage", Toast.LENGTH_SHORT).show();
            Uri uri = data.getData(); // get data uri
            try {
                InputStream inputStream = this.getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                Log.d(TAG, "onActivityResult: "+bitmap);
                imageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    private void classifyImage() throws IOException {
        if(classifier == null){
            recreateClassifier();
        }

        new Runnable() {

            @Override
            public void run() {
                if (classifier != null) {
                    final List<Classifier.Recognition> results = classifier.recognizeImage(rgbBitmap,0);
                    Log.d(TAG, "run: " + results);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Classifier.Recognition recognition = results.get(0);
                            String result = "";
                            float prob = 0f;
                            if (recognition != null) {
                                result = recognition.getTitle();
                                prob = recognition.getConfidence();
                                if (prob > 0.7) tv_result.setText(result);
                                else tv_result.setText("Cannot classify");
                            } else tv_result.setText("Cannot classify");
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