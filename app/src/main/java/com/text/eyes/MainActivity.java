package com.text.eyes;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int CHOOSE_PHOTO = 1222;
    private Button mBtnGallery, mBtnCopy;
    private TextView mTvContent;
    private ImageView mImgThumb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnGallery = findViewById(R.id.btn_gallery);
        mBtnCopy = findViewById(R.id.btn_copy);
        mTvContent = findViewById(R.id.tv_content);
        mImgThumb = findViewById(R.id.img_thumb);
        mBtnGallery.setOnClickListener(this);
        mBtnCopy.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mBtnGallery)) {
            Intent intent = new Intent("android.intent.action.GET_CONTENT");
            intent.setType("image/*");
            startActivityForResult(intent, CHOOSE_PHOTO);//打开相册
        } else if (v.equals(mBtnCopy)) {
            ClipboardManager mClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData myClip = ClipData.newPlainText("text", mTvContent.getText());
            mClipboard.setPrimaryClip(myClip);
            Toast.makeText(MainActivity.this, "copy success", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    handleImageOnKitKat(data);
                }
                break;
            default:
                break;
        }
    }

    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果是document类型的Uri,则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];//解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse
                        ("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //如果是content类型的Uri，则使用普通方式处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //如果是file类型的Uri，直接获取图片路径即可
            imagePath = uri.getPath();
        }
        diaplayImage(imagePath);//根据图片路径显示图片
        pauseImage(uri);

    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        //通过Uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void diaplayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            mImgThumb.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "failed to get iamge", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseImage(Uri uri) {
        FirebaseVisionImage image = null;
        try {
            image = FirebaseVisionImage.fromFilePath(this, uri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        textRecognizer.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText result) {
                        mTvContent.setText(result.getText());
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mTvContent.setText(e.getMessage());
                                Toast.makeText(MainActivity.this, "fail", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
    }

}
