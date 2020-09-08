package com.example.classify_video;

import android.graphics.Bitmap;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FrameExtraction {
    InputStream inp;
    FFmpegFrameGrabber grabber;
    int length;
    public FrameExtraction(InputStream inp, int length){
        this.inp = inp;
        this.length = length;
        grabber = new FFmpegFrameGrabber(inp);
    }

    public List<Bitmap> getListFrame(){
        ArrayList<Bitmap> list = new ArrayList<>();
        try {
            grabber.start();
            AndroidFrameConverter converter = new AndroidFrameConverter();
            int n = grabber.getLengthInFrames();
            int p = n  / length;
            int r = ( n /2)  % p;
            for (int i = 0; i<n;i++){
                Frame frame = grabber.grabImage();
                if (i % p == r) {
                    Bitmap bitmap = converter.convert(frame);
                    list.add(bitmap);
                }
            }
            if (list.size() > length) list.remove(list.size()-1);
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
