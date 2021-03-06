package com.czdxwx.test.utils;

import android.app.Application;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.czdxwx.test.R;
import com.czdxwx.test.MyApplication;
import com.czdxwx.test.model.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * 音乐工具类
 */
public class MusicUtils {
    private static final String TAG = "MusicUtils";

    /**
     * getMusicData()方法扫描系统里的歌曲，放在list集合中
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static List<Song> getMusicData(){
        List<Song> list=new ArrayList<>();
        //查询媒体库并将结果放在cursor对象中
        Cursor cursor= MyApplication.getContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,null,null, MediaStore.Audio.AudioColumns.IS_MUSIC);
        if(cursor!=null){
            while (cursor.moveToNext()){
                Song song=new Song();

                //替换音乐名称的后缀.mp3
                String s=cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
                song.setSong(s.replace(".mp3",""));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    song.setSinger(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));
                }
                song.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
                song.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)));
                song.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)));
                String uri=cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

                //分离出歌曲名和歌手。因为本地媒体库读取的歌曲信息不规范
                if(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))>1000*60){
                    if(song.getSong().contains("-")){
                        String[] str=song.getSong().split("-");
                        song.setSong(str[0]);
                        song.setSinger(str[1]);
                    }
                    list.add(song);
                }
            }
            cursor.close();
        }
        return list;
    }

    /**
     * 定义formatTime()方法用来格式化获取到的时间，因为取得的时间是以毫秒为单位的
     */
    public static String formatTime(int time) {
        if (time / 1000 % 60 < 10) {
            return time / 1000 / 60 + ":0" + time / 1000 % 60;
        } else {
            return time / 1000 / 60 + ":" + time / 1000 % 60;
        }
    }

}
