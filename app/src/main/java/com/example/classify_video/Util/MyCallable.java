package com.example.classify_video.Util;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.classify_video.Activity.ClassifyVideoActivity;
import com.example.classify_video.Classifier.Classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class MyCallable implements Callable<Map<String, Float>> {
    Classifier classifier;
    protected FFmpegMediaMetadataRetriever ff;
    protected  int startTime;
    protected int endTime;
    protected int taskID;
    int numframe;

    public MyCallable(Classifier classifier, FFmpegMediaMetadataRetriever ff, int startTime, int endTime, int taskID) {
        this.classifier = classifier;
        this.ff = ff;
        this.startTime = startTime;
        this.endTime = endTime;
        this.taskID = taskID;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public Map<String, Float> call() throws Exception {
        Log.d("hoa", "run: thread:    "+Thread.currentThread().getName());
        Map<String, Float> map = new HashMap<>();
        ArrayList<String> labels = (ArrayList<String>) ClassifyVideoActivity.labels;
        for (String str : labels) map.put(str, (float) 0);
        Bitmap bitmap;
        numframe = 0;
        Log.d("hoa", "times: "+startTime+"-"+endTime);
        for (int i = startTime; i <= endTime; i+=2) {
            bitmap = ff.getFrameAtTime(i * 1000000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
            if (bitmap != null) {
                Classify(classifier, bitmap, map);
                numframe++;
            }
        }
        Log.d("hoa", "num frame task: "+ taskID +"-"+numframe);
        ClassifyVideoActivity.NUM_FRAME += numframe;
        return map;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void Classify(Classifier classifier, final Bitmap bitmap, Map<String, Float> map) {
        List<Classifier.Recognition> results = classifier.recognizeImage(bitmap, 0);
        Log.d("hoa", "thread "+Thread.currentThread().getName()+"task "+ taskID +" : " + results);
        String classes = "";
        float prob = 0f;
            //tăng % của các class sau mỗi frame
        for (Classifier.Recognition rec : results) {
            classes = rec.getTitle();
            prob = map.get(classes);
            map.replace(classes, prob, prob + rec.getConfidence() * 100);
        }

    }
}
