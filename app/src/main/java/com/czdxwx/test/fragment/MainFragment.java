package com.czdxwx.test.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.czdxwx.test.R;
import com.czdxwx.test.adapter.MainAdapter;
import com.czdxwx.test.model.Main;
import com.czdxwx.test.model.Major;
import com.czdxwx.test.model.Person;
import com.czdxwx.test.utils.ImgUtil;

import net.tsz.afinal.FinalDb;
import net.tsz.afinal.db.sqlite.DbModel;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class MainFragment extends Fragment {
    private View myView;
    private Context mContext;
    private Toolbar mToolbar;
    private ArrayList<Main> settingList = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private MainAdapter mainAdapter;
    private TextView main_name;
    private TextView main_number;

    private static final int IMAGE_REQUEST_CODE = 0;
    private static final int CAMERA_REQUEST_CODE = 1;
    private static final int RESULT_REQUEST_CODE = 2;
    private String[] items = new String[]{"??????????????????", "??????"};
    private static final String IMAGE_FILE_NAME = "pic.jpg";
    // ????????????????????????
    private static final String IMAGE_AVATAR = "avatar.jpg";
    // ??????????????????????????????
    private String imagePath;
    //????????????????????????
    private ImageView imageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        myView = inflater.inflate(R.layout.fragment_main, container, false);
        initPhotoError();
        return myView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mContext = getView().getContext();
        //??????toolbar
        mToolbar = getView().findViewById(R.id.main_toolBar);
        //?????????????????????setSupportActionBar??????????????????????????????????????????????????????????????????setTitle??????
        mToolbar.setTitle("????????????");
        //???toolbar??????actionbar
        mToolbar.inflateMenu(R.menu.add_menu);
        settingList.add(new Main("????????????", "com.czdxwx.ToFontSize"));
        settingList.add(new Main("????????????", "com.czdxwx.ToMajorSet"));
        settingList.add(new Main("????????????", "com.czdxwx.ToMyMusic"));
        settingList.add(new Main("????????????", "com.czdxwx.ToMyVideo"));
        settingList.add(new Main("????????????", "com.czdxwx.ToPay"));
        mainAdapter = new MainAdapter(settingList);
        mainAdapter.setItemClickListener(new MainAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent();
                intent.setAction(settingList.get(position).getIntentInfo());
                startActivity(intent);
            }
        });
        mRecyclerView = getView().findViewById(R.id.main_settingList);
        mLayoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mainAdapter);
        //??????Android??????????????????
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setBackgroundColor(Color.TRANSPARENT);//???????????????
        imageView = getView().findViewById(R.id.imageView2);
        main_name = getView().findViewById(R.id.main_name);
        main_number = getView().findViewById(R.id.main_number);
        main_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction("com.czdxwx.getInformation");
                FinalDb db=FinalDb.create(mContext,"test");
                DbModel p = db.findDbModelBySQL(String.format("select * from person where name='%s';",main_name.getText().toString()));
                Bundle bundle = new Bundle();
                bundle.putString("name", p.getString("name"));
                bundle.putString("number", p.getString("number"));
                bundle.putString("gender", p.getString("gender"));
                bundle.putString("hobby", p.getString("hobby"));
                bundle.putString("nativePlace", p.getString("nativePlace"));
                Toast.makeText(mContext,p.getInt("majorid")+"",Toast.LENGTH_SHORT);
                bundle.putString("major", db.findDbModelBySQL(String.format("select * from Major_info where majorid='%s';",p.getInt("majorid"))).getString("major"));
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }

    @Override
    //???xml??????????????????????????????
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.main_image_menu, menu);
    }

    @Override
    //??????????????????????????????
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_select_image:
                getPicFromLocal();
                break;
            case R.id.main_takePhoto:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getPicFromCamera();
                }
                break;
        }
        return true;
    }

    //???????????????????????????(?????????????????????SD???????????????)
    private static final File USER_ICON = new File(Environment.getExternalStorageDirectory(), "user_icon.jpg");
    //???????????????(?????????????????????????????????????????????)
    private static final int CODE_PHOTO_REQUEST = 1;
    private static final int CODE_CAMERA_REQUEST = 2;
    private static final int CODE_PHOTO_CLIP = 3;

    /**
     * ???????????????????????????
     */
    private void getPicFromLocal() {
        // ???????????????????????????
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CODE_PHOTO_REQUEST);
    }

    private void initPhotoError(){
        // android 7.0???????????????????????????
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            builder.detectFileUriExposure();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getPicFromCamera() {
            Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(mContext, getActivity().getPackageName() + ".fileprovider", USER_ICON);
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(takePhotoIntent, CODE_CAMERA_REQUEST);
    }



    private void photoClip(Uri uri) {
        // ????????????????????????????????????
        Intent intent = new Intent();
        intent.setAction("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        // ????????????crop=true?????????????????????Intent??????????????????VIEW?????????
        intent.putExtra("crop", "true");
        // aspectX aspectY ??????????????????
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        /*outputX outputY ?????????????????????
         *?????????????????????????????????????????????????????????
         * ????????????binder????????????????????????1M??????
         * ???TransactionTooLargeException
         */
        intent.putExtra("outputX", 150);
        intent.putExtra("outputY", 150);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, CODE_PHOTO_CLIP);
    }

    /**
     * ??????????????????????????????????????????????????????????????????View
     */
    private void setImageToHeadView(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Bitmap photo = extras.getParcelable("data");
            imageView.setImageBitmap(photo);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // ????????????????????????????????????????????????
        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(mContext, "??????", Toast.LENGTH_LONG).show();
            return;
        }
        switch (requestCode) {
            case CODE_CAMERA_REQUEST:
                if (USER_ICON.exists()) {
                    photoClip(Uri.fromFile(USER_ICON));
                }
                break;
            case CODE_PHOTO_REQUEST:
                if (data != null) {
                    photoClip(data.getData());
                }
                break;
            case CODE_PHOTO_CLIP:
                if (data != null) {
                    setImageToHeadView(data);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onStart() {
        super.onStart();
        registerForContextMenu(imageView);
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterForContextMenu(imageView);
    }
}