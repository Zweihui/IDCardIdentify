package com.zwh.idcard;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private Button mBtn;
    private TextView mTvId;
    private TextView mTvName;
    private ImageView mIvIdCard;
    private TessBaseAPI mTessBaseAPI;
    private MyHandler mHandler;
    private ProgressDialog mProgressDialog;

    public final static int SHOW_LOADING = 1;
    public final static int HIDE_LOADING = 2;
    public final static int DISPLAY_ID_NUM = 3;
    public final static int DISPLAY_ID_NAME = 4;



    static {
        System.loadLibrary("OpenCV");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new MyHandler(this);
        mBtn = findViewById(R.id.button);
        mTvId = findViewById(R.id.tv_id);
        mTvName = findViewById(R.id.tv_name);
        mIvIdCard = findViewById(R.id.iv_idcard);
        mBtn.setOnClickListener((view) -> {
            recognize();
        });
        mTessBaseAPI = new TessBaseAPI();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED) {
                //没有权限则申请权限
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
                initTess();

            }
        } else {
            initTess();
        }

    }

    public void showLoadingProgress(String message){
        if (mProgressDialog == null){
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setMessage(message);
            mProgressDialog.show();
            return;
        }

        if (mProgressDialog.isShowing()){
            return;
        }else {
            mProgressDialog.setMessage(message);
            mProgressDialog.show();
        }

    }

    public void hideLoadingProgress(){
        if (mProgressDialog.isShowing()){
            mProgressDialog.dismiss();
        }
    }
    public void displayIdNum(String idNum){
        mTvId.setText(idNum);
    }

    public void displayIdName(String name){
        mTvName.setText(name);
    }

    //初始化OCR
    private void initTess() {
        Message message = Message.obtain();
        message.what = SHOW_LOADING;
        message.obj = "资源加载中...";
        mHandler.sendMessage(message);
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
                FileOutputStream fos = null;
                try {
                    is = getAssets().open("ck.traineddata");
                    File file = new File("/sdcard/tess/tessdata/ck.traineddata");
                    if(!file.exists()){
                        file.getParentFile().mkdirs();
                        fos = new FileOutputStream(file);
                        byte[] buffer = new byte[2048];
                        int len;
                        while ((len = is.read(buffer)) != -1){
                            fos.write(buffer,0,len);
                        }
                        fos.close();
                    }
                    is.close();
                    mTessBaseAPI.init("sdcard/tess","ck");
                    Message message3 = Message.obtain();
                    message3.what = HIDE_LOADING;
                    mHandler.sendMessage(message3);
                }catch (IOException e){
                    e.printStackTrace();
                }finally {
                    try {
                        if(null != is){
                            is.close();
                        }
                        if(null != fos){
                            fos.close();
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initTess();

                } else {
                    Toast.makeText(this,"申请权限失败",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


    private void recognize() {
        Message message = Message.obtain();
        message.what = SHOW_LOADING;
        message.obj = "识别中...";
        mHandler.sendMessage(message);
        new Thread(() -> {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.sample);
            Bitmap idNumber = findIdNumber(bitmap, Bitmap.Config.ARGB_8888);
            Bitmap idName = findIdName(bitmap, Bitmap.Config.ARGB_8888);
            bitmap.recycle();
            if (idNumber != null) {
                //OCR文字识别
                mTessBaseAPI.setImage(idNumber);
                Message message1 = Message.obtain();
                message1.what = DISPLAY_ID_NUM;
                message1.obj = mTessBaseAPI.getUTF8Text();
                mHandler.sendMessage(message1);
                mTessBaseAPI.setImage(idName);
                Message message2 = Message.obtain();
                message2.what = DISPLAY_ID_NAME;
                message2.obj = mTessBaseAPI.getUTF8Text();
                mHandler.sendMessage(message2);
                Message message3 = Message.obtain();
                message3.what = HIDE_LOADING;
                mHandler.sendMessage(message3);
            }
        }).start();

    }

    private native Bitmap findIdName(Bitmap bitmap, Bitmap.Config argb8888);

    private native Bitmap findIdNumber(Bitmap bitmap, Bitmap.Config argb8888);


    private static class MyHandler extends Handler{
        private final WeakReference<MainActivity> mActivity;
        public MyHandler(MainActivity activity){
            mActivity = new WeakReference<MainActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg){
            MainActivity activity = mActivity.get();
            if(activity != null){
                switch (msg.what){
                    case SHOW_LOADING:
                        activity.showLoadingProgress((String) msg.obj);
                        break;
                    case HIDE_LOADING:
                        activity.hideLoadingProgress();
                        break;
                    case DISPLAY_ID_NUM:
                        activity.displayIdNum((String) msg.obj);
                        break;
                    case DISPLAY_ID_NAME:
                        activity.displayIdName((String) msg.obj);
                        break;
                        default:
                            break;
                }
            }
        }
    }

}
