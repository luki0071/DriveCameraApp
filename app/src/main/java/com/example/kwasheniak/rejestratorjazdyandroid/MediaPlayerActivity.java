package com.example.kwasheniak.rejestratorjazdyandroid;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class MediaPlayerActivity extends AppCompatActivity implements
        SurfaceHolder.Callback, MediaPlayer.OnPreparedListener,
        MediaController.MediaPlayerControl {

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private MediaPlayer mediaPlayer;
    private TextView mSubtitlesTextView;
    private RelativeLayout mRelativeLayout;
    private MediaController mediaController;
    private Handler mHandler = new Handler();
    private ArrayList<Integer> czas;
    private ArrayList<String> napisy;
    private File mVideoSource;
    private File mSubtitlesSource;
    private boolean isPrepared = false;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i("My","landscape");
            setVideoSize();

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.i("My", "portrait");
            setVideoSize();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        mSubtitlesTextView = (TextView) findViewById(R.id.textView);
        mRelativeLayout = (RelativeLayout) findViewById(R.id.relativeLayoutMediaPlayer);
        surfaceView = (SurfaceView)findViewById(R.id.surfaceview);
        mRelativeLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(mediaController != null){
                    mediaController.show();
                }
                return false;
            }
        });
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        czas = new ArrayList<Integer>();
        napisy = new ArrayList<String>();
        setVideoSources();
        setSubtitles();

        mediaPlayer = new MediaPlayer();
        mediaController = new MediaController(this);
        try {
            mediaPlayer.setDataSource(mVideoSource.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setSubtitles(){
        if(mSubtitlesSource.exists()){
            try {
                BufferedReader br = new BufferedReader(new FileReader(mSubtitlesSource));
                String[] data;
                String line;

                while ((line = br.readLine()) != null) {
                    if(line.length() != 0){
                        data = line.split("-");
                        try{
                            czas.add(Integer.parseInt(data[0]));
                        }catch(NumberFormatException e){
                            e.printStackTrace();
                        }
                        napisy.add(data[1]);
                    }
                }
                br.close();
            }
            catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Nie udało się wczytać napisów", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Toast.makeText(this, "Brak napisów do tego nagrania", Toast.LENGTH_SHORT).show();
        }
    }

    public void setVideoSources(){
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String directoryVideoPath = extras.getString("DIRECTORY_VIDEO_PATH");
            String fileName = extras.getString("FILE_NAME");
            mVideoSource = new File(directoryVideoPath,fileName + ".mp4");
            mSubtitlesSource = new File(directoryVideoPath,fileName + ".txt");
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            setVideoSize();
            mediaPlayer.setDisplay(surfaceHolder);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnPreparedListener(this);
            if(!isPrepared){
                mediaPlayer.prepare();
            }
        } catch (Exception e) {

            final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Błąd");
            dialog.setMessage("Nie można odtworzyć nagrania: " + e.toString());
            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.show();
        }
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mediaPlayer.start();
        mediaController.setMediaPlayer(this);
        mediaController.setAnchorView(mRelativeLayout);
        startShowSubtitles();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        }
    }

    private Runnable mHandlerTask = new Runnable()
    {
        @Override
        public void run() {
            mHandler.postDelayed(mHandlerTask, 1000);
            if(mediaPlayer.isPlaying()){
                int index = 0;
                int currentTime = mediaPlayer.getCurrentPosition();
                for(Integer i : czas){
                    if(currentTime < i){
                        mSubtitlesTextView.setText(napisy.get(index));
                        break;
                    }
                    index += 1;
                }
            }
            else{
                mHandler.removeCallbacks(mHandlerTask);
            }
        }
    };

    public void startShowSubtitles()
    {
        mHandlerTask.run();
    }


    private void setVideoSize() {
        MediaMetadataRetriever mdr = new MediaMetadataRetriever();
        mdr.setDataSource(mVideoSource.getAbsolutePath());
        int videoHeight = Integer.parseInt(mdr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)); // 1200
        int videoWidth = Integer.parseInt(mdr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)); // 1600
        int videoOrientation = Integer.parseInt(mdr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        float videoProportion = (float) videoWidth / (float) videoHeight; //1600 / 1200

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x; // 1080
        int screenHeight = size.y; // 1776

        ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();

        if(this.getResources().getConfiguration().orientation == 1){ //portrait
            float screenProportion = (float) screenHeight / (float) screenWidth; // 1776 / 1080
            if(videoProportion == screenProportion) {
                if (videoOrientation == 90) {
                    lp.width = screenWidth; // 1080
                    lp.height = screenHeight; // 1920
                } else {
                    lp.width = screenWidth; //1080
                    lp.height = (int) ((float) screenWidth / videoProportion); // 1080 / 1,777774
                }
            }
            else if(videoProportion > screenProportion){
                if (videoOrientation == 90) {
                    lp.width = (int) ((float) screenHeight/ videoProportion); // 1920 / 1,77774
                    lp.height = screenHeight; // 1920
                } else {
                    lp.width = screenWidth; //1080
                    lp.height = (int) ((float) screenWidth / videoProportion); // 1080 / 1,777774
                }
            }
            else{
                if (videoOrientation == 90) {
                    lp.width = screenWidth; // 1080
                    lp.height = (int) ((float) screenHeight / videoProportion); // 1920 / 1,77774
                } else {
                    lp.width = screenHeight; //1080
                    lp.height = (int) ((float) screenWidth / videoProportion); // 1080 / 1,777774
                }
            }
        }
        else if(this.getResources().getConfiguration().orientation == 2){ // horizontal
            float screenProportion = (float) screenWidth / (float) screenHeight; // 1794 / 1080
            if(videoProportion == screenProportion) {
                if(videoOrientation == 90){
                    lp.width = (int) ((float) screenHeight / videoProportion); //1080 / 1,7774
                    lp.height = screenHeight; // 1080
                }
                else{
                    lp.width = screenWidth; // 1920
                    lp.height = screenHeight; // 1080
                }
            }
            else if(videoProportion > screenProportion){
                if (videoOrientation == 90) {
                    lp.width = (int) ((float) screenHeight / videoProportion); // 1080 / 1,77774
                    lp.height = screenHeight; // 1080
                } else {
                    lp.width = screenWidth; //1920
                    lp.height = (int) ((float) screenWidth / videoProportion); // 1080
                }
            }
            else{
                if (videoOrientation == 90) {
                    lp.width = (int) ((float) screenHeight / videoProportion); // 1080 / 1,77774
                    lp.height = screenHeight; // 1080
                } else {
                    lp.width = (int) ((float) screenWidth / videoProportion); //1920 / 1,7774
                    lp.height = screenHeight; // 1080 / 1,777774
                }
            }
        }
        surfaceView.setLayoutParams(lp);
    }

    @Override
    public void start() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mediaPlayer.start();
        startShowSubtitles();
    }

    @Override
    public void pause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return mediaPlayer.getAudioSessionId();
    }
}
