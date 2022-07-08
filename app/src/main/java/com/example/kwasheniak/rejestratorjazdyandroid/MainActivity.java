package com.example.kwasheniak.rejestratorjazdyandroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button mCameraButton;
    private Button mVideosFolderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraButton = (Button) findViewById(R.id.buttonCamera);
        mVideosFolderButton = (Button) findViewById(R.id.buttonVideosFolder);

        mCameraButton.setOnClickListener(new View.OnClickListener() { // otwiera okno kamery
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        mVideosFolderButton.setOnClickListener(new View.OnClickListener() { // otwiera okno folderu z nagraniami
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RecordedVideosActivity.class);
                startActivity(intent);
            }
        });
    }
}
