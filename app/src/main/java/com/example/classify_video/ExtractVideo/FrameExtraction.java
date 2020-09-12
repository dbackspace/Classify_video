package com.example.classify_video.ExtractVideo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;


import com.example.classify_video.Util.MapUtil;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class FrameExtraction {

    FFmpegMediaMetadataRetriever ff = new FFmpegMediaMetadataRetriever();

    public FrameExtraction(String path) {
        ff.setDataSource(path);

    }

    public FrameExtraction(Uri uri, Activity context) {
        File file = new File(MapUtil.getRealPathFromURI(context,uri));
        ff.setDataSource(file.getAbsolutePath());
    }

    public List<Bitmap> getListFrame() {
        ArrayList<Bitmap> list = new ArrayList<>();
        int length = ff.getMetadata().getInt(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
        Bitmap bitmap;
        length = length / 1000;
        for (int i = 0; i <= length; i++) {
            bitmap = ff.getFrameAtTime(i * 1000000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
            if (bitmap != null)
                list.add(bitmap);
            else {
                Log.d("tuan", Integer.toString(i));
            }
        }
        return list;
    }
}