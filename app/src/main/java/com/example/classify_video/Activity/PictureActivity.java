package com.example.classify_video.Activity;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.classify_video.Classifier.Classifier;
import com.example.classify_video.Database.MyDatabase;
import com.example.classify_video.Database.MyImage;
import com.example.classify_video.Database.MyVideo;
import com.example.classify_video.R;
import com.example.classify_video.Util.BitmapUtil;
import com.example.classify_video.Util.MapUtil;

import org.tensorflow.lite.support.common.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class PictureActivity extends AppCompatActivity {
    public static String TAG = "PictureActivity";
    private static final int SELECT_GALLERY_IMAGE = 3;
    //    LinearLayout btn_listImage, btn_select;
    CircleImageView imgv_select, imgv_listClassify;
    TextView tv_result;
    ProgressBar progressBar;
    ImageView imageView;
    Bitmap bitmap = null;
    Classifier classifier;
    MyDatabase database;
    private String image_path = null;
    private String title = null;
    private int check = 0;
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);
        imgv_select = findViewById(R.id.imgv_selectimg);
        imgv_listClassify = findViewById(R.id.imgv_listimgClassify);
        tv_result = findViewById(R.id.tv_image_result);
        progressBar = findViewById(R.id.pr_process2);
        imageView = findViewById(R.id.imgv_pl);
        database = new MyDatabase(this);
        Intent intent = getIntent();

        check = intent.getIntExtra("CHECK",0);
        // neu sang bang cach chup anh
        if(check == 1){
            bitmap = MainActivity.bitmap;
            imageView.setImageBitmap(bitmap);
            try {
                classifyImage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{// sang bang cach chon file
            check = 0;
            image_path = intent.getStringExtra("image_path");
            if (image_path != null) {
                Log.d(TAG, "receive path: "+image_path);
                bitmap = BitmapFactory.decodeFile(image_path);
                imageView.setImageBitmap(bitmap);
                MyImage image_check = database.getImageByPath(image_path);
                /**
                 * video đã phân loại thì  đường dẫn đã được lưu vào db
                 */
                if (image_check != null) {
                    title = image_check.getType();
                    tv_result.setText("Hình ảnh đã được phân loại.\nChủ đề: " + title);
                } else {
                    try {
                        if (bitmap != null) {
                            classifyImage(bitmap);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        /**
         *         select from gallery
         */
        imgv_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadImageFromStorage();

            }
        });
        /**
         * show list image at the same type
         */
        imgv_listClassify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "send to list image: "+title+"-"+image_path);
                Intent intent1 = new Intent(PictureActivity.this, ListImageActivity.class);
                intent1.putExtra("image_label", title);
                intent1.putExtra("image_path", image_path);
                startActivity(intent1);
            }
        });
    }

    private void loadImageFromStorage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, SELECT_GALLERY_IMAGE);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        if (requestCode == SELECT_GALLERY_IMAGE) {
            Uri uri = data.getData();
            image_path = MapUtil.getRealPathFromURI(this,uri);
            bitmap = BitmapUtil.getBitmapFromGallery(this, uri);
            imageView.setImageBitmap(bitmap);

            MyImage image_check = database.getImageByPath(image_path);
            /**
             * video đã phân loại thì  đường dẫn đã được lưu vào db
             */
            if (image_check != null) {
                title = image_check.getType();
                tv_result.setText("Hình ảnh đã được phân loại.\nChủ đề: " + title);
            } else {
                try {
                    if (bitmap != null) {
                        classifyImage(bitmap);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void classifyImage(Bitmap bitmap1) throws IOException {
        if (classifier == null) {
            recreateClassifier();
        }
        long start = System.currentTimeMillis();
        Map<String, Float> map = new HashMap<>();
        ArrayList<String> labels = (ArrayList<String>) FileUtil.loadLabels(this, "labels.txt");
        for (String str : labels) map.put(str, (float) 0);
        MapUtil.Classify(classifier, bitmap1, map);
        map = MapUtil.sortByValue(map);
        List<Classifier.Recognition> recognitionList = Classifier.getTopKProbability(map, 3);
        String final_result = "";
        for (Classifier.Recognition recognition : recognitionList) {
            final_result += recognition.getTitle() + " : " + String.format("(%.1f%%) ", recognition.getConfidence()) + "\n";
        }
        long end = System.currentTimeMillis();
        long t = end - start;
        final_result += "time: " + t / 1000 + "s";
//        progressBar.setVisibility(View.GONE);
        Log.d(TAG, "check list recog: " + recognitionList.size());
        if (recognitionList.get(0).getConfidence() >= 60.0) {
            tv_result.setText(final_result);
            title = recognitionList.get(0).getTitle();
            //luu vao db
            if(image_path == null){
                image_path = saveImage(bitmap1,"/"+title);
                Log.d(TAG, "save imagepath: "+image_path);
            }
            MyImage image = new MyImage(image_path, title);
            database.addImage(image);
        } else {
            tv_result.setText("Không phân loại được\n" + "time: " + t / 1000 + "s");
        }
    }

    public String saveImage(Bitmap myBitmap,String title) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(
                Environment.getExternalStorageDirectory() + title);
        // have the object build the directory structure, if needed.

        if (!wallpaperDirectory.exists()) {
            Log.d("dirrrrrr", "" + wallpaperDirectory.mkdirs());
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".jpg");
            f.createNewFile();   //give read write permission
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Toast.makeText(this, "Save Image success", Toast.LENGTH_SHORT).show();
           // startActivity(new Intent(this, ListImageActivity.class));
            Log.d("TAG", "File Saved::--->" + f.getAbsolutePath());

            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";

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
    protected void onResume() {
        super.onResume();
//        Toast.makeText(this,"onResume of PictureActivity",Toast.LENGTH_LONG).show();
    }
}