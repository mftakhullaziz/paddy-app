package com.example.paddydiseaseclassificationapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ResultActivity extends AppCompatActivity {

    private TextView textViewResult1;
    private TextView textViewResult2;
    private Classifier classifier;

    ImageView imageView1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        textViewResult1 = findViewById(R.id.tv1);
        textViewResult2 = findViewById(R.id.tv2);

//        final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
//        textViewResult1.setText(results.toString().replace("[", "").replace("]", ""));

    }
}