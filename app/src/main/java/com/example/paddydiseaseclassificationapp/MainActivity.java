package com.example.paddydiseaseclassificationapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String MODEL_PATH = "model.tflite";
    private static final boolean QUANT = true;
    private static final String LABEL_PATH = "labels1.txt";
    private static final int INPUT_SIZE = 224;

    private Classifier classifier;

    private Executor executor = Executors.newSingleThreadExecutor();

    private static final int PERMISSION_CODE = 1000;
    private static final int IMAGE_CAPTURE_CODE = 1001;

    int REQUEST_GALLERY = 1;
    int REQUEST_CAMERA = 100;

    Button mCaptureBtn;
    Button mLoadBtn;
    ImageView mImageView;
    TextView mTv;

    Uri image_uri;

    Button mPredict_image_btn;
    ProgressDialog progressDialog;

    Button mResults_image_btn;

    private int PICK_IMAGE_REQUEST = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.image_view);
        mCaptureBtn = findViewById(R.id.capture_image_btn1);
        mLoadBtn = findViewById(R.id.capture_image_btn2);
        mPredict_image_btn = findViewById(R.id.predict_image_btn);
        mResults_image_btn = findViewById(R.id.results_image_btn);
        mTv = findViewById(R.id.textView1);

        mCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if system os is >= marshmallow, request runtime permission
                if (checkSelfPermission(Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_DENIED ||
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                PackageManager.PERMISSION_DENIED){
                    //permission not enabled, request it
                    String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    //show popup to request permissions
                    requestPermissions(permission, PERMISSION_CODE);
                }
                else {
                    //permission already granted
                    openCamera();
                }
            }
        });


        mLoadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, PICK_IMAGE_REQUEST);


            }
        });


        if(mPredict_image_btn.isClickable()){
            initTensorFlowAndLoadModel();
//            finish();
            mPredict_image_btn.setOnClickListener(new View.OnClickListener() {

                @SuppressLint("HandlerLeak")
                Handler handle = new Handler() {
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        progressDialog.incrementProgressBy(2); // Incremented By Value 2
                    }
                };

                @Override
                public void onClick(View v) {

                    if (mImageView.getDrawable() == null){
                        Toast.makeText(getBaseContext(), "Please upload/capture a picture first !" , Toast.LENGTH_SHORT ).show();
                    }
                    else{
                        progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                //Put your AlertDialog Here ....
                                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                                alert.setMessage("predict disease from image done !");
                                alert.show();

                            }
                        });

                        progressDialog.setMax(100); // Progress Dialog Max Value
                        progressDialog.setTitle("The process of predicting disease !"); // Setting Title
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL); // Progress Dialog Style Horizontal
                        progressDialog.show(); // Display Progress Dialog
                        progressDialog.setCancelable(false);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {

                                    while (progressDialog.getProgress() <= progressDialog.getMax()) {
                                        Thread.sleep(100);
                                        handle.sendMessage(handle.obtainMessage());
                                        if (progressDialog.getProgress() == progressDialog.getMax()) {
                                            progressDialog.dismiss();
                                            mImageView.invalidate();
                                            BitmapDrawable drawable = (BitmapDrawable) mImageView.getDrawable();
//                                        Bitmap bitmap = Bitmap.createBitmap(drawable.getBitmap());
                                            Bitmap bitmap = drawable.getBitmap();
                                            bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
                                            //imageViewResult.setImageBitmap(bitmap);

                                            final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
                                            mTv.setText(results.toString().replace("[", "\n\n\n").replace("]", "\n"));
                                        }
                                    }


                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                        }).start();
                    }
                }
            });
        } //


        mResults_image_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,  ResultActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_PATH,
                            LABEL_PATH,
                            INPUT_SIZE,
                            QUANT);
                    makeButtonVisible();
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    private void makeButtonVisible() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPredict_image_btn.setVisibility(View.VISIBLE);
            }
        });
    }


    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        //Camera intent
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }

    //handling permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //this method is called, when user presses Allow or Deny from Permission Request Popup
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                //permission from popup was granted
                openCamera();
            } else {
                //permission from popup was denied
                Toast.makeText(this, "Permission denied...", Toast.LENGTH_SHORT).show();
            }
        }
    }

        @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //called when image was captured from camera

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            //set the image captured to our ImageView
            mImageView.setImageURI(image_uri);




//            image_uri = data.getData();
//            try {
//                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), image_uri);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            if (requestCode == REQUEST_CAMERA) {
                if (data != null) {
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    /* Passing BITMAP to the Second Activity */
//                    Intent IntentCamera = new Intent(this, ResultsActivity.class);
//                    IntentCamera.putExtra("BitmapImage", photo);
//                    startActivity(IntentCamera);
                }
            } else if (requestCode == REQUEST_GALLERY) {
                if (data != null) {
//                    image_uri = data.getData();
                    /* Passing ImageURI to the Second Activity */
//                    Intent IntentGallery = new Intent(this, ResultsActivity.class);
//                    IntentGallery.setData(image_uri);
//                    startActivity(IntentGallery);
                    try {
                        final Uri imageUri = data.getData();
                        final InputStream imageStream;
                        imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        setPicture(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }


        }
//        else if (requestCode == REQUEST_GALLERY && requestCode == PICK_IMAGE_REQUEST) {
//            try {
//                final Uri imageUri = data.getData();
//                final InputStream imageStream;
//                imageStream = getContentResolver().openInputStream(imageUri);
//                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
//                setPicture(selectedImage);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public void setPicture(Bitmap bp)
    {
        Bitmap scaledBp =  Bitmap.createScaledBitmap(bp, mImageView.getWidth(), mImageView.getHeight(), false);
        mImageView.setImageBitmap(scaledBp);
    }
}
