package com.example.classify_video;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class FrameExtraction {

    FFmpegMediaMetadataRetriever ff = new FFmpegMediaMetadataRetriever();

    public FrameExtraction(String path){
        ff.setDataSource(path);

    }

    public FrameExtraction(Uri uri, Context context){
        ff.setDataSource(context,uri);
    }

    public List<Bitmap> getListFrame(){
        ArrayList<Bitmap> list = new ArrayList<>();
        int length = ff.getMetadata().getInt(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
        Bitmap  bitmap;
        length = length / 1000;
        for ( int i =0 ;i <= length; i++){
            bitmap = ff.getFrameAtTime(i * 1000000,FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
            list.add(bitmap);
        }
        return list;
    }
}
