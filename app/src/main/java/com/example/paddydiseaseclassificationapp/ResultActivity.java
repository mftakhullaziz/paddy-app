package com.example.paddydiseaseclassificationapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

public class ResultActivity extends AppCompatActivity {

    ImageView imageView1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        ImageView image = (ImageView) findViewById(R.id.image_view_results);

        Bundle bundle = getIntent().getExtras();
        if ( bundle != null){
            int res = bundle.getInt("image");
            image.setImageResource(res);
        }
    }
}