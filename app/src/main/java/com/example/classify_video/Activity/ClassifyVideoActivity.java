package com.example.classify_video.Activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.classify_video.Classifier.Classifier;
import com.example.classify_video.Database.MyDatabase;
import com.example.classify_video.Database.MyVideo;
import com.example.classify_video.Database.VideoAdapter;
import com.example.classify_video.R;
import com.example.classify_video.Util.MapUtil;
import com.example.classify_video.Util.MyCallable;

import org.tensorflow.lite.support.common.FileUtil;

import java.io.File;
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
public class ClassifyVideoActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "Classify";
    private static final int REQUEST_PERMISSION_RESULT = 0;
    private static final int NUM_THREAD = 5;// số luồng chạy đồng thời trong threadpool
    private static final int MAX_THREAD = 10;// số task tối đa đc tạo ra
    private String newPath = null;
    public static int NUM_FRAME = 0;
    int REQUEST_CODE_FOLDER = 456;
    public static Uri videoUri;
    VideoView videoView;
    TextView tv_label;
    ImageView imgv_select, imgv_classify, imgv_rt;
    private ProgressBar progressBar;
    private int position = 0;
    private String title = "", videoPath = "",savePath = null;
    MediaController mediaController;
    List<Bitmap> listBitmap;
    Classifier classifier;
    public static List<String> labels;
    Map<String, Float> map;
    MyDatabase database;
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classify_video);
        videoView = findViewById(R.id.video_view);
        tv_label = findViewById(R.id.tv_label);
        imgv_select = findViewById(R.id.imgv_selectVideo);
        imgv_classify = findViewById(R.id.imgv_classify);
        imgv_rt = findViewById(R.id.imgv_listClassify);
        progressBar = findViewById(R.id.progress);
        listBitmap = new ArrayList<>();
        database = new MyDatabase(this);
//        tv_label.setVisibility(View.VISIBLE);
        map = new HashMap<>();
        try {
            labels = FileUtil.loadLabels(this, "labels.txt");
//            Log.d(TAG, "onCreate: " + labels.size());
            for (String str : labels) map.put(str, (float) 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /**
         * chuyển từ main activity sang
         */
        Intent intent = getIntent();
        videoPath= intent.getStringExtra("video_path");
        Log.d(TAG, "from main activity: "+videoPath);
        if (videoPath != null) {
            videoView.setVideoURI(Uri.parse(videoPath));
            videoView.start();
//            ClassifyVideo(videoMain_path);
        }
        if (videoUri != null) {
            Log.d("TAG", " resume video uri: " + videoUri);
            videoView.setVideoURI(videoUri);
            videoView.start();
        }
//        if (this.mediaController == null) {
//            this.mediaController = new MediaController(ClassifyVideoActivity.this);
//            this.mediaController.setAnchorView(videoView);
//            this.videoView.setMediaController(mediaController);
//        }
//        this.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            public void onPrepared(MediaPlayer mediaPlayer) {
//                videoView.seekTo(position);
//                if (position == 0) {
//                    videoView.start();
//                }
//            }
//        });


        imgv_select.setOnClickListener(this);
        imgv_classify.setOnClickListener(this);
        imgv_rt.setOnClickListener(this);
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
    private void ClassifyVideo(String videoPath) {
        imgv_classify.setEnabled(false);
        long start = System.currentTimeMillis();
        NUM_FRAME = 0;
        if (ExtractVideo(videoPath) == 1) {
            //sắp xếp các giá trị dự đoán
            map = MapUtil.sortByValue(map);
            map = MapUtil.div_Map(map, NUM_FRAME);
            Log.d(TAG, "num frame: " + NUM_FRAME);
            Log.d(TAG, "final_result: " + map.toString());
            //
            List<Classifier.Recognition> recognitionList = Classifier.getTopKProbability(map, 3);
            String final_result = "";
            for (Classifier.Recognition recognition : recognitionList) {
                final_result += recognition.getTitle() + " : " + String.format("(%.1f%%) ", recognition.getConfidence()) + "\n";
            }
            long end = System.currentTimeMillis();
            long t = end - start;
            final_result += "time: " + t / 1000 + "s";
            Log.d(TAG, "check list recog: " + recognitionList.size());
            if (recognitionList.get(0).getConfidence() > 50.0) {
                tv_label.setText(final_result);
                title = recognitionList.get(0).getTitle();
                //luu vao db
                MyVideo video = new MyVideo(videoPath, title);
                database.addVideo(video);
//                if(adapter == null){
//                    adapter = new VideoAdapter(this,video_recommend);
//                }
//                adapter.notifyDataSetChanged();
            } else {
                tv_label.setText("Không phân loại được\n" + "time: " + t / 1000 + "s");
            }

            listBitmap.clear();
            map = MapUtil.resetMap(map);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private int ExtractVideo(String videoPath) {
        if (classifier == null) {
            recreateClassifier();
        }
        if (videoPath == null){
            Toast.makeText(this, "Chọn video để thực hiện phân loại", Toast.LENGTH_SHORT).show();
        }
        else {
            Log.d(TAG, "ExtractVideo: " + videoPath);
            MyVideo video_check = database.getVideoByPath(videoPath);
            /**
             * video đã phân loại thì  đường dẫn đã được lưu vào db
             */
            if (video_check != null) {
                title = video_check.getType();
                tv_label.setText("Video đã được phân loại.\nChủ đề: "+title);
//                Toast.makeText(this, "Video đã được phân loại.\nChủ đề: "+title, Toast.LENGTH_LONG).show();
                return 0;
            }
            /**
             * Ngược lại
             */
            else {
                FFmpegMediaMetadataRetriever ff = new FFmpegMediaMetadataRetriever();
                ff.setDataSource(videoPath);
                ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREAD);
                List<Future<Map<String, Float>>> listFuture = new ArrayList<Future<Map<String, Float>>>();
                //
                int videoLength = ff.getMetadata().getInt(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
                videoLength = videoLength / 1000;
                int min_length = videoLength / MAX_THREAD;// số giây tối thiểu để phân loại
                Log.d("hoa", "video length: " + videoLength);
                if (videoLength >= min_length) {
                    int part_length = Math.round((float) videoLength / MAX_THREAD);
                    int start_time = 0;
                    int end_time = part_length - 1;
                    Log.d("hoa", "part_length: " + part_length);
                    MyCallable myCallable = new MyCallable(classifier, ff, start_time, end_time, 1);
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

                        myCallable = new MyCallable(classifier1, ff, start_time, end_time, i);
                        future = executorService.submit(myCallable);
                        listFuture.add(future);
                        i++;
                    }
                } else {
                    MyCallable myCallable = new MyCallable(classifier, ff, 0, videoLength, 1);
                    Future<Map<String, Float>> future = executorService.submit(myCallable);
                    listFuture.add(future);
                }
                for (Future future : listFuture) {
                    try {
                        Log.d(TAG, "map : " + future.get());
                        Map<String, Float> tmp_map = (Map<String, Float>) future.get();
                        for (String str : map.keySet()) {
                            map.put(str, map.get(str) + tmp_map.get(str));
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
        return 1;
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgv_selectVideo:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_PICK);
                intent.setType("video/mp4");
                startActivityForResult(intent, REQUEST_CODE_FOLDER);
                videoPath = null;
                //
//                tv_label.setVisibility(View.VISIBLE);
                imgv_classify.setEnabled(true);
                break;
            case R.id.imgv_listClassify:
//                Toast.makeText(this, "Phan loai video realtime", Toast.LENGTH_LONG).show();
                Intent intent1 = new Intent(ClassifyVideoActivity.this,ListVideoActivity.class);
                intent1.putExtra("video_label",title);
                intent1.putExtra("video_path",videoPath);
                startActivity(intent1);
                break;
            case R.id.imgv_classify:

                tv_label.setText("Đang phân loại....");
                Log.d(TAG, "onClick: "+tv_label.getText());
                progressBar.setVisibility(View.VISIBLE);
                if(videoPath!=null){
                    ClassifyVideo(videoPath);
                }else if(videoUri!=null){
                    String videoPath = MapUtil.getRealPathFromURI(this, videoUri);
                    ClassifyVideo(videoPath);
                }
                progressBar.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.btn_save) {
            if (videoPath != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ClassifyVideoActivity.this);
                builder.setTitle("Lưu video");
                builder.setMessage("Bạn có muốn lưu video này vào thư mục: " + title + " không?");
                builder.setPositiveButton("Có", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //lưu video vào thư mục
                        CreateFolder(title);
                        MyVideo video2 = database.getVideoByPath(videoPath);
                        SaveVideo(video2.getId(), title);
                        Toast.makeText(ClassifyVideoActivity.this,"Lưu thành công vào thư mục: "+savePath,Toast.LENGTH_LONG).show();
                    }
                });
                builder.setNegativeButton("Không", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }

        }

        return super.onOptionsItemSelected(item);
    }
    private void CreateFolder(String folder_name) {
        File file = new File(Environment.getExternalStorageDirectory(), folder_name);
        //kiểm tra tồn tại
        if (file.exists()) {
//            Toast.makeText(this, "folder đã tồn tại", Toast.LENGTH_SHORT).show();
        } else {
            //tạo mới folder
            file.mkdirs();
            //ktra điều kiện
            if (file.isDirectory()) {
                //folder đã đc tạo
//                Toast.makeText(this, "Folder đã được tạo thành công", Toast.LENGTH_LONG).show();
            } else {
                //folder không đc tạo
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                String messages = "Message: Lỗi khi tạo folder " +
                        "\nPath : " + Environment.getExternalStorageDirectory() +
                        "\nmkdirs : " + file.mkdirs();
                builder.setMessage(messages);
                builder.show();
            }
        }
    }
    private void SaveVideo(int video_id, String title1) {
        String video_name = videoPath.substring(videoPath.lastIndexOf("/") + 1);
        String toPath = Environment.getExternalStorageDirectory() + File.separator + title1 + File.separator + video_name;
        File from = new File(videoPath);
        File to = new File(toPath);
        savePath = toPath;
        from.renameTo(to);
        videoPath = toPath;
        videoUri = Uri.parse(videoPath);
        //update database
        if (database.updateVideo(video_id, videoPath)) {
//            Toast.makeText(this, "Lưu video thành công", Toast.LENGTH_LONG).show();
        } else {
//            Toast.makeText(this, "Lưu video không thành công", Toast.LENGTH_LONG).show();
        }
        Log.d(TAG, "SaveVideo: " + video_id + "-" + videoPath);
        newPath = videoPath;
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        if(newPath!=null){
//            videoView.setVideoURI(videoUri);
//            videoView.start();
//        }
//    }
}