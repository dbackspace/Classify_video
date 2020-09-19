package com.example.classify_video.Activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.classify_video.Classifier.Classifier;
import com.example.classify_video.R;
import com.example.classify_video.Util.MapUtil;
import com.example.classify_video.Util.MyCallable;

import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * chọn video từ thư mục điện thoại
 * tách video ra frame
 * phân loại video
 */
public class ClassifyVideoActivity extends AppCompatActivity {
    private static final String TAG = "Classify";
    private static final int REQUEST_PERMISSION_RESULT = 0;
    private static final int NUM_THREAD = 5;// số luồng chạy đồng thời trong threadpool
    private static final int MAX_THREAD = 10;// số luồng tối đa đc tạo ra
    public static  int NUM_FRAME = 0;
    int REQUEST_CODE_FOLDER = 456;
    boolean permission = false;
    public static Uri videoUri;
    VideoView videoView;
    TextView tv_label;
    ImageView  imgv_select, imgv_classify;
    int position = 0;
    MediaController mediaController;

    List<Bitmap> listBitmap;
    Classifier classifier;
    public static List<String> labels;
    Map<String, Float> map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classify_video);
        videoView = findViewById(R.id.video_view);
        tv_label = findViewById(R.id.tv_label);
        imgv_select = findViewById(R.id.imgv_select);
        imgv_classify = findViewById(R.id.imgv_classify);
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

        //chọn video từ điện thoại
        imgv_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_PICK);
                intent.setType("video/mp4");
                startActivityForResult(intent, REQUEST_CODE_FOLDER);
                tv_label.setText("");
            }
        });
        //click nút ở giữa để phân loại
        imgv_classify.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {

                long start = System.currentTimeMillis();
                NUM_FRAME = 0;
                ExtractVideo();

                //sắp xếp các giá trị dự đoán
                map = MapUtil.sortByValue(map);
                map = MapUtil.div_Map(map, NUM_FRAME);
                Log.d(TAG, "num frame: "+NUM_FRAME);
                Log.d(TAG, "final_result: " + map.toString());
                //
                List<Classifier.Recognition> recognitionList = Classifier.getTopKProbability(map, 3);
                String final_result = "";
                for (Classifier.Recognition recognition : recognitionList) {
                    final_result += recognition.getTitle() + " : " + String.format("(%.1f%%) ", recognition.getConfidence()) + "\n";
                }
                long end = System.currentTimeMillis();
                long t = end - start;
                final_result+="time: "+t/1000+"s";
                Log.d(TAG, "check list recog: "+recognitionList.size());
                if (recognitionList.get(0).getConfidence() >= 80.0) {
                    tv_label.setText(final_result);
                } else {
                    tv_label.setText("Không phân loại được\n"+"time: "+t/1000+"s");
                }

                listBitmap.clear();
                map = MapUtil.resetMap(map);


            }

//            }
        });
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void ExtractVideo() {
        if (classifier == null) {
            recreateClassifier();
        }
        if (videoUri == null)
            Toast.makeText(this, "Chọn video để thực hiện phân loại", Toast.LENGTH_SHORT).show();
        else {
            String videoPath = MapUtil.getRealPathFromURI(this, videoUri);
            FFmpegMediaMetadataRetriever ff = new FFmpegMediaMetadataRetriever();
            ff.setDataSource(videoPath);
            ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREAD);
            List<Future<Map<String, Float>>> listFuture = new ArrayList<Future<Map<String, Float>>>();
            //
            int videoLength = ff.getMetadata().getInt(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
            videoLength = videoLength / 1000;
            int min_length = videoLength/MAX_THREAD;// số giây tối thiểu để phân loại
            Log.d("hoa", "video length: " + videoLength);
            if (videoLength >= min_length) {
                int part_length = Math.round((float) videoLength / MAX_THREAD);
//                int part_length = 40;
                int start_time = 0;
                int end_time = part_length - 1;
                Log.d("hoa", "part_length: " + part_length);
                MyCallable myCallable = new MyCallable(classifier,ff,start_time,end_time,1);
                Future<Map<String, Float>> future = executorService.submit(myCallable);
                listFuture.add(future);

                int i = 2;
                while (end_time < videoLength) {
                    start_time = end_time + 1;
                    if (start_time + part_length > videoLength) end_time = videoLength;
                    else {
                        end_time = start_time + part_length;
                    }
                    Classifier classifier1 = null;
                    try {
                        classifier1 = Classifier.create(this);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    myCallable = new MyCallable(classifier1,ff,start_time,end_time,i);
                    future = executorService.submit(myCallable);
                    listFuture.add(future);
                    i++;
                }
            } else {
                MyCallable myCallable = new MyCallable(classifier,ff,0,videoLength,1);
                Future<Map<String, Float>> future = executorService.submit(myCallable);
                listFuture.add(future);
            }
            for (Future future : listFuture) {
                try {
                    Log.d(TAG, "map : "+future.get());
                    Map<String ,Float> tmp_map = (Map<String, Float>) future.get();
                    for(String str:map.keySet()){
                        map.put(str,map.get(str)+tmp_map.get(str));
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            executorService.shutdown();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_FOLDER) {
                videoUri = data.getData();
                Log.d("TAG", "video path: " + videoUri);
                if (videoUri != null) {
                    videoView.setVideoURI(videoUri);
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