package com.example.classify_video.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;


public class MyDatabase extends SQLiteOpenHelper {
    Context context;
    private static String DBName = "Classifier.db";
    private String id = "id";
    private String table_video = "video_tbl";
    private String table_image = "image_tbl";
    private String video_path = "video_path";
    private String image_path = "image_path";
    private String type = "type";
    public MyDatabase(Context context) {
        super(context, DBName, null,1);
        this.context = context;
        Log.d("TAG", "MyDatabase: createDB");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createVideo_tbl = String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT)",table_video,id,video_path,type);
        String createImage_tbl = String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT)",table_image,id,image_path,type);
        db.execSQL(createVideo_tbl);
        db.execSQL(createImage_tbl);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String droptableVideo = String.format("Drop table if exist %s", table_video);
        String droptableImage = String.format("Drop table if exist %s", table_image);
        db.execSQL(droptableVideo);
        db.execSQL(droptableImage);
        onCreate(db);
    }

    /**
     *
     * @param video
     */
    public void addVideo(MyVideo video){
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values  = new ContentValues();
        values.put(video_path,video.getVideo_path());
        values.put(type,video.getType());
        database.insert(table_video,null,values);
        Log.d("TAG", "addVideo: success - type: "+video.getType());
    }
    public ArrayList<MyVideo> getVideoByType(String type){
        SQLiteDatabase database = getReadableDatabase();
        ArrayList<MyVideo> list = new ArrayList<>();
        String sql = "SELECT * FROM video_tbl WHERE type = ?";
        Cursor cursor = database.rawQuery(sql, new String[]{type});
        if(cursor.moveToFirst()){
            do{
                MyVideo video = new MyVideo();
                video.setId(cursor.getInt(0));
                video.setVideo_path(cursor.getString(1));
                video.setType(cursor.getString(2));
                list.add(video);
            }while(cursor.moveToNext());
        }
        return list;
    }
    public MyVideo getVideoByPath(String path){
        SQLiteDatabase database = getReadableDatabase();
        String sql = "SELECT * FROM video_tbl WHERE video_path = ?";
        Cursor cursor = database.rawQuery(sql, new String[]{path});
        MyVideo video = new MyVideo();
        if(cursor.moveToFirst()){
            video.setId(cursor.getInt(0));
            video.setVideo_path(cursor.getString(1));
            video.setType(cursor.getString(2));
        }
        if(video.getType() == null) return null;
        return video;
    }
    public boolean updateVideo(int video_id, String path){
        Log.d("Classify", "updateVideo: "+video_id+"-"+path);
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(video_path,path);
        return database.update(table_video,values, " id = ?", new String[]{String.valueOf(video_id)}) > 0;
    }
    public ArrayList<MyVideo> getAllVideo() {
        SQLiteDatabase database = getReadableDatabase();
        ArrayList<MyVideo> list = new ArrayList<>();
        String sql = "SELECT * FROM video_tbl";
        Cursor cursor = database.rawQuery(sql, null);
        if(cursor.moveToFirst()){
            do{
                MyVideo video = new MyVideo();
                video.setId(cursor.getInt(0));
                video.setVideo_path(cursor.getString(1));
                video.setType(cursor.getString(2));
                list.add(video);
            }while(cursor.moveToNext());
        }
        return list;
    }
    /**
     *
     * @param img
     */
    public void addImage(MyImage img){
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values  = new ContentValues();
        values.put(image_path,img.getImage_path());
        values.put(type,img.getType());
        database.insert(table_image,null,values);
        Log.d("TAG", "addImage: success - type: "+img.getType());
    }
    public ArrayList<MyImage> getImageByType(String type){
        SQLiteDatabase database = getReadableDatabase();
        ArrayList<MyImage> list = new ArrayList<>();
        String sql = "SELECT * FROM "+table_image+" WHERE type = ?";
        Cursor cursor = database.rawQuery(sql, new String[]{type});
        if(cursor.moveToFirst()){
            do{
                MyImage img = new MyImage();
                img.setId(cursor.getInt(0));
                img.setImage_path(cursor.getString(1));
                img.setType(cursor.getString(2));
                list.add(img);
            }while(cursor.moveToNext());
        }
        return list;
    }
    public MyImage getImageByPath(String path){
        SQLiteDatabase database = getReadableDatabase();
        String sql = "SELECT * FROM image_tbl WHERE image_path = ?";
        Cursor cursor = database.rawQuery(sql, new String[]{path});
        MyImage img = new MyImage();
        if(cursor.moveToFirst()){
            img.setId(cursor.getInt(0));
            img.setImage_path(cursor.getString(1));
            img.setType(cursor.getString(2));
        }
        if(img.getType() == null) return null;
        return img;
    }
    public boolean updateImage(int img_id, String path){
        Log.d("Classify", "updateVideo: "+img_id+"-"+path);
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(image_path,path);
        return database.update(table_image,values, " id = ?", new String[]{String.valueOf(img_id)}) > 0;
    }

    public ArrayList<MyImage> getAllImage() {
        SQLiteDatabase database = getReadableDatabase();
        ArrayList<MyImage> list = new ArrayList<>();
        String sql = "SELECT * FROM "+table_image;
        Cursor cursor = database.rawQuery(sql,null);
        if(cursor.moveToFirst()){
            do{
                MyImage img = new MyImage();
                img.setId(cursor.getInt(0));
                img.setImage_path(cursor.getString(1));
                img.setType(cursor.getString(2));
                list.add(img);
            }while(cursor.moveToNext());
        }
        return list;
    }


}
