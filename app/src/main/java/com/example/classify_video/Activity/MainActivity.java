package com.example.classify_video.Activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Picture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.example.classify_video.R;
import com.example.classify_video.Util.BitmapUtil;
import com.example.classify_video.Util.CameraPreview;
import com.example.classify_video.Util.MapUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_CAMERA = 1;
    private static final int CAMERA_PIC_REQUEST = 2;
    private static final int SELECT_GALLERY_IMAGE = 3;
    private Camera mCamera = null;
    private CameraPreview mPreview;
    private Camera.PictureCallback mPicture;
    private LinearLayout btnStorage, btnCapture, btnClassify;
    private Context mContext;
    private CircleImageView imgThumb;
    private LinearLayout cameraPreview;
    private ProgressBar progressBar;
    public static Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnCapture = findViewById(R.id.btn_capture);
        btnClassify = findViewById(R.id.btn_classify);
        btnStorage = findViewById(R.id.btn_storage);
        imgThumb = findViewById(R.id.imgThumb);
        progressBar = findViewById(R.id.pr_process);
        btnStorage.setOnClickListener(this);
        btnClassify.setOnClickListener(this);
        btnCapture.setOnClickListener(this);

        checkPermission();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mContext = this;
        ArrayList<String> images = BitmapUtil.getAllShownImagesPath(MainActivity.this);
        try {
            if (images.size() > 0) {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.fromFile(new File(images.get(images.size() - 1)))));
                imgThumb.setImageBitmap(bitmap);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                mCamera.takePicture(null,null,mPicture);
            }
        });

    }
    private void checkPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CAMERA);
        } else {
            Log.d(TAG, "checkPermission: vao 1");
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
            cameraPreview = findViewById(R.id.cameraPreview);
            if (mPreview == null) {
                mPreview = new CameraPreview(this, mCamera);
            }
            mPreview = new CameraPreview(this, mCamera);
            cameraPreview.addView(mPreview);
            mCamera.startPreview();

            mCamera.setDisplayOrientation(90);
            mPicture = getPictureCallback();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: vao day");
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);
        cameraPreview = (LinearLayout) findViewById(R.id.cameraPreview);
        if (mPreview == null) {
            mPreview = new CameraPreview(this, mCamera);
        }
        mPreview = new CameraPreview(this, mCamera);
        cameraPreview.addView(mPreview);
        mCamera.startPreview();
        mCamera.setDisplayOrientation(90);
        mPicture = getPictureCallback();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


    }
    public void onResume() {

        super.onResume();
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onResume: da check");
            if (mCamera == null) {

                if (mPreview == null) {
                    mPreview = new CameraPreview(this, mCamera);
                }


                Log.d(TAG, "onResume: vao 2");
                mCamera = Camera.open();
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();

                mPreview.refreshCamera(mCamera);
                Log.d("nu", "null");
            } else {
                Log.d("nu", "no null????");
            }
        } else {
            Log.d("nu", "no null");
        }


    }
    @Override
    protected void onPause() {
        super.onPause();
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();
    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }
    private Camera.PictureCallback getPictureCallback(){
        Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                bitmap = BitmapUtil.rotation(bitmap,90);
                imgThumb.setImageBitmap(bitmap);
                // chuyen sang activity phan loai
                Intent intent = new Intent(MainActivity.this, PictureActivity.class);
                intent.putExtra("CHECK",1);
                startActivity(intent);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                },1000);
            }
        };
        return pictureCallback;
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_storage:
                loadImageFromStorage();
                break;
            case R.id.btn_classify:
                loadImageClassify();
                break;
            default: break;
        }
    }
    private void loadImageClassify(){
        Intent intent = new Intent(MainActivity.this, ListImageActivity.class);
        intent.putExtra("All_image",2);
        startActivity(intent);
    }
    private void loadImageFromStorage(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("*/*");
        startActivityForResult(intent, SELECT_GALLERY_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data == null) return;
        if(requestCode == SELECT_GALLERY_IMAGE){
            Uri uri = data.getData();
            String mediaPath = MapUtil.getRealPathFromURI(this,uri);
            Log.d(TAG, "onActivityResult: "+ mediaPath+"-"+uri);
//            String extension = mediaPath.split(".")[1];
            if(!mediaPath.contains("mp4")){
                bitmap = BitmapUtil.getBitmapFromGallery(this,data.getData());
                Intent intent = new Intent(MainActivity.this,PictureActivity.class);
//                intent.putExtra("CHECK",1);
                intent.putExtra("image_path",mediaPath);
                startActivity(intent);
            }else{
                Intent intent = new Intent(MainActivity.this, ClassifyVideoActivity.class);
                intent.putExtra("video_path",mediaPath);
                startActivity(intent);
            }

        }
    }
}