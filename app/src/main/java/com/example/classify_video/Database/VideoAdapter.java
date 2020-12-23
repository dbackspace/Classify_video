package com.example.classify_video.Database;

import android.content.Context;
import android.graphics.Bitmap;
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

import java.util.ArrayList;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder>  {
    private ArrayList<MyVideo> videos;
    itemClickIntf activity;
    private Context context;


    public interface itemClickIntf{
        void itemClicked(int position, ArrayList<MyVideo> myvideos);
    }

    public VideoAdapter(Context context, ArrayList<MyVideo> videos) {
        this.videos = videos;
        activity = (itemClickIntf) context;
        this.context = context;
    }
    public class ViewHolder extends RecyclerView.ViewHolder{
//        VideoView videoView;
        ImageView imageView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
//            videoView = itemView.findViewById(R.id.video_view);
            imageView = itemView.findViewById(R.id.image_view);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.itemClicked(videos.indexOf((MyVideo)v.getTag()),videos);
                }
            });
        }
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.itemView.setTag(videos.get(position));
        String path = videos.get(position).getVideo_path();
//        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(path,
//                MediaStore.Video.Thumbnails.MICRO_KIND);
//        Log.d("TAG", "onBindViewHolder: "+(thumbnail == null));
//        holder.imageView.setImageBitmap(thumbnail);
        Log.d("VideoAdapter", "video_path: "+path);
        Uri videoURI = Uri.parse(path);
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context.getApplicationContext(), videoURI);
        Bitmap thumb = retriever.getFrameAtTime(50, MediaMetadataRetriever.OPTION_PREVIOUS_SYNC);
        holder.imageView.setBackground(new BitmapDrawable(context.getResources(), thumb));

    }

    @Override
    public int getItemCount() {
        return videos.size();
    }
}
