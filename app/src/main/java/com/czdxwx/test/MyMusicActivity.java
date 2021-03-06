package com.czdxwx.test;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.czdxwx.test.adapter.SongAdapter;
import com.czdxwx.test.model.Song;
import com.czdxwx.test.service.MusicService;
import com.czdxwx.test.utils.MusicUtils;

import java.util.ArrayList;
import java.util.List;

public class MyMusicActivity extends AppCompatActivity implements View.OnClickListener {

    private static List<Song> songList = new ArrayList<>();
    private MusicService.MusicBinder musicBinder;
    private static final String TAG = "MyMusicActivity";
    private SongAdapter adapter;
    private ImageView songImage;
    private TextView songName;
    private TextView songSinger;
    private ImageButton previous;
    private ImageButton play;
    private ImageButton next;
    private static final int UPDATE = 0;
    private Thread myThread;
    private GestureDetector detector;
    //设置屏幕的最小滑动量
    protected static final float FLIP_DISTANCE = 150;
    private Toolbar toolbar;

    private int pos = -1;

    public static List<Song> getSongList() {
        return songList;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_music);

        //开启并绑定服务
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, connection, BIND_AUTO_CREATE);

        //实例化各控件
        songImage = (ImageView) findViewById(R.id.song_image);
        songName = (TextView) findViewById(R.id.song_name);
        songSinger = (TextView) findViewById(R.id.song_singer);
        previous = (ImageButton) findViewById(R.id.music_previous);
        play = (ImageButton) findViewById(R.id.music_play);
        next = (ImageButton) findViewById(R.id.music_next);
        previous.setOnClickListener(this);
        play.setOnClickListener(this);
        next.setOnClickListener(this);
        songImage.setOnClickListener(this);
        //隐藏默认actionbar
        ActionBar actionBar = this.getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        //实例化Toolbar，并让他有ActionBar的功能
        toolbar = findViewById(R.id.myMusic_toolbar);
        toolbar.setTitle("我的音乐");
        setActionBar(toolbar);
        getActionBar().setDisplayHomeAsUpEnabled(true);//显示toolbar的返回按钮
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //运行时权限处理，判断用户是否授权过权限，如果没有就申请该权限
        //如果已经授权过了，就调用initSongs()方法进行初始化操作
        if (ContextCompat.checkSelfPermission(MyMusicActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MyMusicActivity.this, new String[]
                    {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            initSongs();//初始化歌曲信息
        }

        //实例化RecyclerView
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.main_recyclerView);
        //设置RecyclerView 的布局方式为线性布局
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        //实例化SongAdapter，并为RecyclerView设置实例化的SongAdapter对象
        adapter = new SongAdapter(songList);
        recyclerView.setAdapter(adapter);
        //设置RecyclerView子项点击事件
        adapter.setListener(new SongAdapter.MyItemClickListener() {
            @Override
            public void onItemClick(int position) {
                pos = position;
                initMediaPlayer();
                play.setImageResource(R.drawable.play);
                musicBinder.setMediaPlayer(position);
            }
        });

        //实例化GestureDetector对象并设置监听事件
        detector = new GestureDetector(MyMusicActivity.this, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1.getX() - e2.getX() > FLIP_DISTANCE) {
                    startMyActivity();
                    Toast.makeText(MyMusicActivity.this, "左滑", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    /**
     * 重写onCreateOptionsMenu()方法允许菜单显示出来
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.music_menu, menu);
        MenuItem menuItem =menu.findItem(R.id.search_music);//在菜单中找到对应控件的item
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }


    /**
     * 定义startMyActivity()方法用于启动PlayShowActivity
     */
    private void startMyActivity() {
        //判断当前系统的版本，选择合适的启动方式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //共享元素启动，两个活动使用相同的专辑图片，启动时会有相应的动画效果
            Intent intent = new Intent(MyMusicActivity.this, MusicPlayShowActivity.class);
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, songImage, "image");
            startActivity(intent, options.toBundle());
        } else {
            //一般启动
            Intent intent = new Intent(MyMusicActivity.this, MusicPlayShowActivity.class);
            startActivity(intent);
        }
    }

    //使用handler接收线程发出的消息，通知是否进行数据更新
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE) {//只要msg.arg1!=pos，就说明音乐信息发生改变，需要更新UI
                if (msg.arg1 != pos) {
                    pos = msg.arg1;
                    initMediaPlayer();
                }
            }
        }
    };

    //定义一个线程用于帮助handler实时更新UI
    private class MyThread implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (MusicService.mediaPlayer != null) {
                    Message message = new Message();
                    message.what = UPDATE;
                    //取得服务正在播放的音乐的position参数
                    message.arg1 = MusicService.getPos();
                    handler.sendMessage(message);
                    try {
                        //每过500毫秒就更新一次
                        Thread.sleep(500);
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        }
    }

    //实现活动与服务的绑定，让活动能够操作服务做某些事情
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicBinder = (MusicService.MusicBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    /**
     * initSongs()方法调用MusicUtils工具类的getMusicData()获取本地音乐信息，并用
     * List<Song>数组进行接收，然后foreach遍历list数组并将数据添加到songList中
     */
    private void initSongs() {
        List<Song> list = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            list = MusicUtils.getMusicData();
        }
        for (Song song : list) {
            songList.add(song);
        }
        //开启线程
        myThread = new Thread(new MyThread());
        myThread.start();
    }

    /**
     * 对请求权限后用户的操作进行判断：如果用户同意授权，就调用initSongs()方法
     * 如果用户拒绝授权，就弹出一条权限获取失败提示
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initSongs();
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, "您拒绝了该权限将无法使用本软件的某些功能！", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    /**
     * 调用initMediaPlayer()方法对播放界面的UI进行设置
     */
    private void initMediaPlayer() {
        if (songList != null) {
            songName.setText(songList.get(pos).getSong());
            songSinger.setText(songList.get(pos).getSinger());
        }
    }

    /**
     * 设置点击事件的逻辑
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.music_play:
                musicBinder.playMusic();
                if (MusicService.mediaPlayer.isPlaying()) {
                    play.setImageResource(R.drawable.play);
                } else {
                    play.setImageResource(R.drawable.pause);
                }
                break;
            case R.id.music_previous:
                musicBinder.previousMusic();
                play.setImageResource(R.drawable.play);
                break;
            case R.id.music_next:
                musicBinder.nextMusic();
                play.setImageResource(R.drawable.play);
                break;
            case R.id.song_image:
                startMyActivity();
            default:
        }
    }
    protected void onStart() {
        super.onStart();
        adapter.getFilter().filter("");
    }
    /**
     * 在活动销毁时对服务进行解绑和停止线程
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        songList.clear();
        myThread.interrupt();
        unbindService(connection);
    }
}