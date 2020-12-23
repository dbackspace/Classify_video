package com.example.classify_video.Database;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.classify_video.R;
import com.example.classify_video.Util.BitmapUtil;

import java.util.ArrayList;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private ArrayList<MyImage> images;
    itemClickIntf activity;
    private Context context;


    public interface itemClickIntf {
        void itemClicked(int position, ArrayList<MyImage> myImagess);
    }

    public ImageAdapter(Context context, ArrayList<MyImage> images) {
        this.images = images;
        activity = (itemClickIntf) context;
        this.context = context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.itemClicked(images.indexOf((MyImage) v.getTag()), images);
                }
            });
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.itemView.setTag(images.get(position));
        String path = images.get(position).getImage_path();
        Log.d("ImageAdapter", "video_path: " + path);
        Bitmap thumb = BitmapFactory.decodeFile(path);
        holder.imageView.setImageBitmap(thumb);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }
}
