package com.dabai.piccc;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.tapadoo.alerter.Alerter;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.filter.Filter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    //图片集合
    List<Bitmap> bitmaps;
    Bitmap bitres;
    ImageView iv1;
    private File file;

    boolean save_ok;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setElevation(0);

        /**
         * 申请权限
         */
        int checkResult = getApplicationContext().checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //if(!=允许),抛出异常
        if (checkResult != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100); // 动态申请读取权限
            }
        } else {
        }

        iv1 = findViewById(R.id.imageView1);
        bitmaps = new ArrayList<>();


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Matisse.from(MainActivity.this)
                        .choose(MimeType.ofImage(), true)
                        .countable(true)
                        .maxSelectable(8)
                        .gridExpectedSize(300)
                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                        .thumbnailScale(0.85f)
                        .imageEngine(new GlideEngine())
                        .forResult(101);

            }
        });
    }


    List<Uri> mSelected;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {

            bitmaps.clear();

            mSelected = Matisse.obtainResult(data);
            if (mSelected.size() < 8) {
                Alerter.create(this)
                        .setTitle("提示")
                        .setText("图片数量不够8个，不能拼接")
                        .setBackgroundColor(R.color.colorAccent)
                        .show();
                return;
            }

            if (Build.VERSION.SDK_INT >= 19) {

                for (Uri uri : mSelected) {
                    handleImageOnkitKat(uri);//高于4.4版本使用此方法处理图片
                }

            } else {

                for (Uri uri : mSelected) {
                    handleImageBeforeKitKat(uri);//低于4.4版本使用此方法处理图片
                }
            }

            Bitmap[] bitmaparray = new Bitmap[bitmaps.size()];
            for (int i = 0; i < bitmaps.size(); i++) {
                bitmaparray[i] = bitmaps.get(i);
            }

            Bitmap bit1, bit2;

            bit1 = add2Bitmap(bitmaparray[0], bitmaparray[1]);
            bit1 = add2Bitmap(bit1, bitmaparray[2]);
            bit1 = add2Bitmap(bit1, bitmaparray[3]);
            //第一排完成合并

            bit2 = add2Bitmap(bitmaparray[4], bitmaparray[5]);
            bit2 = add2Bitmap(bit2, bitmaparray[6]);
            bit2 = add2Bitmap(bit2, bitmaparray[7]);
            //第二排完成合并

            bitres = combineImage(bit1, bit2);
            iv1.setImageBitmap(bitres);

            save_ok = true;

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {

            if (save_ok) {
                save_img();
            } else {

                Alerter.create(this)
                        .setTitle("提示")
                        .setText("还没有拼接，不能保存。")
                        .setBackgroundColor(R.color.colorAccent)
                        .show();

            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {


        if (requestCode == 100) {
            /**
             * 申请权限
             */
            int checkResult = getApplicationContext().checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (checkResult != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "没有权限,不能运行", Toast.LENGTH_SHORT).show();
                finish();
            } else {
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    /**
     *  
     * 横向拼接 
     * <功能详细描述> 
     *
     * @param first 
     * @param second 
     * @return 
     */
    private Bitmap add2Bitmap(Bitmap first, Bitmap second) {
        int width = first.getWidth() + second.getWidth();
        int height = Math.max(first.getHeight(), second.getHeight());
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(first, 0, 0, null);
        canvas.drawBitmap(second, first.getWidth(), 0, null);
        return result;
    }


    /**
     * 拼接图片
     *
     * @param bitmaps 原图片集
     * @return 拼接后的新图
     */
    public static Bitmap combineImage(Bitmap... bitmaps) {
        boolean isMultiWidth = false;//是否为多宽度图片集
        int width = 0;
        int height = 0;

        //获取图纸宽度
        for (Bitmap bitmap : bitmaps) {
            if (width != bitmap.getWidth()) {
                if (width != 0) {//过滤掉第一次不同
                    isMultiWidth = true;
                }
                width = width < bitmap.getWidth() ? bitmap.getWidth() : width;
            }
        }

        //获取图纸高度
        for (Bitmap bitmap : bitmaps) {
            if (isMultiWidth) {
                height = height + bitmap.getHeight() * width / bitmap.getWidth();
            } else {
                height = height + bitmap.getHeight();
            }
        }

        //创建图纸
        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //创建画布,并绑定图纸
        Canvas canvas = new Canvas(newBitmap);
        int tempHeight = 0;
        //画图
        for (int i = 0; i < bitmaps.length; i++) {
            if (isMultiWidth) {
                if (width != bitmaps[i].getWidth()) {
                    int newSizeH = bitmaps[i].getHeight() * width / bitmaps[i].getWidth();
                    Bitmap newSizeBmp = resizeBitmap(bitmaps[i], width, newSizeH);
                    canvas.drawBitmap(newSizeBmp, 0, tempHeight, null);
                    tempHeight = tempHeight + newSizeH;
                    newSizeBmp.recycle();
                } else {
                    canvas.drawBitmap(bitmaps[i], 0, tempHeight, null);
                    tempHeight = tempHeight + bitmaps[i].getHeight();
                }
            } else {
                canvas.drawBitmap(bitmaps[i], 0, tempHeight, null);
                tempHeight = tempHeight + bitmaps[i].getHeight();
            }
            bitmaps[i].recycle();
        }
        return newBitmap;
    }


    public static Bitmap resizeBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        float scaleWidth = ((float) newWidth) / bitmap.getWidth();
        float scaleHeight = ((float) newHeight) / bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bmpScale = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return bmpScale;
    }

    @TargetApi(19)
    private void handleImageOnkitKat(Uri uri) {
        String imagePath = null;

        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果是document类型的uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];//解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android,providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }

        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        }
        displayImage(imagePath);
    }

    private void handleImageBeforeKitKat(Uri uri) {
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    //获得图片路径
    public String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);   //内容提供器
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));   //获取路径
                } catch (Exception e) {

                }
            }
        }
        cursor.close();
        return path;
    }


    //展示图片
    private void displayImage(String picturePath) {
        if (picturePath != null) {

            Bitmap bitmap = getLoacalBitmap(picturePath); //从本地取图片(在cdcard中获取)  //
            bitmaps.add(bitmap);

        } else {
            Toast.makeText(this, "获取图片失败,请换一个图片选择器试试!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 加载本地图片
     *
     * @param url
     * @return
     */
    public static Bitmap getLoacalBitmap(String url) {
        try {
            FileInputStream fis = new FileInputStream(url);
            return BitmapFactory.decodeStream(fis);  ///把流转化为Bitmap图片

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


    //检查sd
    public static boolean checkSDCardAvailable() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    public static void savePhotoToSDCard(Bitmap photoBitmap, String path, String photoName) {
        if (checkSDCardAvailable()) {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File photoFile = new File(path, photoName + ".png");
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(photoFile);
                if (photoBitmap != null) {
                    if (photoBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)) {
                        fileOutputStream.flush();
                    }
                }
            } catch (FileNotFoundException e) {
                photoFile.delete();
                e.printStackTrace();
            } catch (IOException e) {
                photoFile.delete();
                e.printStackTrace();
            } finally {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //根据view获取bitmap
    public static Bitmap getBitmapByView(View view) {
        int h = 0;
        Bitmap bitmap = null;
        bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.RGB_565);
        final Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }


    public void save_img() {

        try {
            setTitle("图片拼接 - 保存中...");
            new Thread(new Runnable() {
                @Override
                public void run() {

                    Bitmap bitmap = getBitmapByView(iv1);//iv是View
                    int ran = new Random().nextInt(1000);
                    savePhotoToSDCard(bitmap, "/sdcard/图片拼接", "PICC_" + ran);
                    file = new File("/sdcard/图片拼接/PICC_" + ran + ".png");

                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Alerter.create(MainActivity.this)
                                    .setTitle("提示")
                                    .setText("保存到" + file.getAbsolutePath() + "成功")
                                    .setBackgroundColor(R.color.colorAccent)
                                    .show();
                            setTitle("图片拼接");
                        }
                    });

                }
            }).start();


        } catch (Exception e) {
            Alerter.create(this)
                    .setTitle("提示")
                    .setText("保存失败" + e)
                    .setBackgroundColor(R.color.colorAccent)
                    .show();
        }

    }


}
