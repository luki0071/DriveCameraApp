package com.example.kwasheniak.rejestratorjazdyandroid;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class CameraActivity extends AppCompatActivity implements LocationListener,
        GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "MyActivity";
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;
    private static final int REQUEST_ACCESS_FINE_LOCATION = 1;
    private final static int INTERVAL = 500;
    private long UPDATE_INTERVAL = 1000;
    private long FASTEST_INTERVAL = 1000;

    private TextureView mTextureView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (isEmptyTable(mDatabaseHelper.TABLE_SETTINGS)) {
                setupCamera(width, height);
            } else {
                Cursor dataSettings = mDatabaseHelper.getDataSettings();
                dataSettings.moveToFirst();
                Size size = Size.parseSize(dataSettings.getString(1));
                setupCamera(size.getWidth(), size.getHeight());
                dataSettings.close();
            }
            //transformImage(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private Handler mHandler = new Handler();

    private String mCameraId;
    private Size mPreviewSize;
    private Size mVideoSize;
    private MediaRecorder mMediaRecorder;

    private CaptureRequest.Builder mCaptureRequestBuilder;

    private Button mRecordButton;
    private boolean mIsRecording = false;

    private File mVideoFolderInternal;
    private File mVideoFolderExternal;
    private String mVideoFileName;
    private String mCommonName;
    private boolean mIsInternal = true;

    private Button mShowParamsButton;

    private Size[] mCameraResolutions;

    private Button mFreeStorageButton;
    private Button mSettingsButton;
    private Button mSpeedButton;
    private Button mDistanceButton;

    private Chronometer mChronometer;

    private DatabaseHelper mDatabaseHelper;

    private File mFreeStorageSpace;
    private Long mLimitStorage;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    private Location mLastLocation = null;
    private Long mTimeOfLastLocation = null;
    private Long mStartTime;
    private Float mSpeed = (float)0;
    private Float mDistance;

    private ArrayList<String> mSubtitles = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_camera);

        mDatabaseHelper = new DatabaseHelper(this);
        mMediaRecorder = new MediaRecorder();

        mFreeStorageButton = (Button) findViewById(R.id.buttonFreeStorage);
        createVideoFolder();
        saveDefaultStorageLimit();

        isGooglePlayServicesAvailable();

        if(!isLocationEnabled())
            showAlert();

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mSpeedButton = (Button) findViewById(R.id.buttonSpeed);
        mDistanceButton = (Button) findViewById(R.id.buttonDistance);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);

        mTextureView = (TextureView) findViewById(R.id.textureView);
        mRecordButton = (Button) findViewById(R.id.buttonRecord);
        recordVideoOnClick();

        hideParamsOnClick();

        mSettingsButton = (Button) findViewById(R.id.buttonSettings);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CameraActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        createVideoFolder();
        startBackgroundThread();

        if(mTextureView.isAvailable()){
            Cursor data = mDatabaseHelper.getDataSettings();
            data.moveToFirst();
            Size size = Size.parseSize(data.getString(1));
            setupCamera(size.getWidth(), size.getHeight());
            transformImage(mTextureView.getWidth(), mTextureView.getHeight());
            data.close();
            connectCamera();
        }
        else{
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Nie przyznano dostępu do kamery", Toast.LENGTH_SHORT).show();
            }
            if(grantResults[1] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Nie przyznano dostępu do audio", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Przyznano dostęp do pamięci", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Aplikacja nie ma dostępu do pamięci", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == REQUEST_ACCESS_FINE_LOCATION){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(CameraActivity.this, "Dostęp do odczytu lokalizacji przyznany", Toast.LENGTH_LONG).show();
                try{
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                } catch (SecurityException e) {
                    Toast.makeText(CameraActivity.this, "Nie można odczytać lokalizacji :\n" + e.toString(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(CameraActivity.this, "Nie przyznano dostępu do odczytu lokalizacji", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(mIsRecording) {
            if(mGoogleApiClient.isConnected()){
                mGoogleApiClient.disconnect();
            }
            mChronometer.stop();
            mChronometer.setTextColor(Color.WHITE);
            mRecordButton.setBackgroundResource(R.drawable.cast_ic_expanded_controller_play);
            mSettingsButton.setEnabled(true);
            stopRepeatingTask();
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaStoreUpdateIntent.setData(Uri.fromFile(new File(mVideoFileName)));
            sendBroadcast(mediaStoreUpdateIntent);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mIsRecording = false;
        }
        closeCamera();
        stopBackgroundThread();
    }

    public boolean isSDCardExists(){
        boolean flag = false;
        try{
            ContextCompat.getExternalFilesDirs(this, null)[1].toString();
            showFreeMemory();
            flag = true;
        }catch(Exception e){
            Cursor data = mDatabaseHelper.getDataSettings();
            data.moveToFirst();
            if(!isEmptyTable(mDatabaseHelper.TABLE_SETTINGS)){
                if(data.getString(5).toString().equals("Karta SD")){
                    SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(mDatabaseHelper.COLUMN_SAVELOCATION_SETTINGS, "Pamięć wewnętrzna");

                    db.update(mDatabaseHelper.TABLE_SETTINGS, contentValues, "ID = 1", null);

                    db.close();

                    AlertDialog.Builder mBuilder = new AlertDialog.Builder(CameraActivity.this);
                    mBuilder.setTitle("Uwaga!");
                    mBuilder.setMessage("Nie wykryto karty SD, miejsce zapisu zmienione na pamięć wewnętrzną");
                    mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alertDialog = mBuilder.create();
                    alertDialog.show();
                    showFreeMemory();
                }
            }
            data.close();
        }
        return flag;
    }

    public void showFreeMemory(){

        if(!isEmptyTable(mDatabaseHelper.TABLE_SETTINGS)){
            Cursor data = mDatabaseHelper.getDataSettings();
            data.moveToFirst();
            File[] movieFile = ContextCompat.getExternalFilesDirs(CameraActivity.this, null);

            if(data.getString(6).toString().equals("ON")){
                mFreeStorageButton.setVisibility(View.VISIBLE);
                if(data.getString(5).toString().equals("Pamięć wewnętrzna")){
                    Float valueInMB = (float)movieFile[0].getFreeSpace() / (1024*1024);
                    mFreeStorageButton.setText(String.format("%.0f", valueInMB) + " MB");
                }
                else if(data.getString(5).toString().equals("Karta SD")){
                    Float valueInMB = (float)movieFile[1].getFreeSpace() / (1024*1024);
                    mFreeStorageButton.setText(String.format("%.0f", valueInMB) + " MB");
                }
            }
            else{
                mFreeStorageButton.setVisibility(View.GONE);
            }
            data.close();
        }
    }

    private Runnable mHandlerTask = new Runnable()
    {
        @Override
        public void run() {
            mHandler.postDelayed(mHandlerTask, INTERVAL);
            showFreeMemory();
            long freeSpace = mFreeStorageSpace.getFreeSpace() / (1024*1024);
            if(freeSpace <= mLimitStorage){
                if(mGoogleApiClient.isConnected()){
                    mGoogleApiClient.disconnect();
                }
                mChronometer.stop();
                mChronometer.setTextColor(Color.WHITE);
                mIsRecording = false;
                mRecordButton.setBackgroundResource(R.drawable.cast_ic_expanded_controller_play);
                mSettingsButton.setEnabled(true);
                startPreview();
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaStoreUpdateIntent.setData(Uri.fromFile(new File(mVideoFileName)));
                sendBroadcast(mediaStoreUpdateIntent);
                final AlertDialog.Builder dialog = new AlertDialog.Builder(CameraActivity.this);
                dialog.setTitle("Uwaga");
                dialog.setMessage("Pamięć urządzenia osiągnęła limit nagrywania. Nagrywanie zostało zatrzymane, a zarejestrowane dotychczas nagranie zapisane.");
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                dialog.show();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                mHandler.removeCallbacks(mHandlerTask);
            }
        }
    };

    public void startRepeatingTask()
    {
        mHandlerTask.run();
    }

    public void stopRepeatingTask() { mHandler.removeCallbacks(mHandlerTask); }

    public void hideParamsOnClick(){
        mShowParamsButton = (Button) findViewById(R.id.buttonShowParams);
        final LinearLayout table = (LinearLayout) findViewById(R.id.params_layout);
        table.setVisibility(View.GONE);

        mShowParamsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(table.getVisibility() == View.GONE){
                    table.setVisibility(View.VISIBLE);
                }
                else{
                    table.setVisibility(View.GONE);
                }
            }
        });
    }

    public void recordVideoOnClick(){
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsRecording){
                    if(mGoogleApiClient.isConnected()){
                        mGoogleApiClient.disconnect();
                    }
                    if(mSubtitles.size() != 0) {
                        mSubtitles.add(String.valueOf(System.currentTimeMillis() - mStartTime) + "-" +
                                mSpeed.toString() + "Km/H  " +
                                mDistance.toString() + "Km " + String.valueOf(mLastLocation.getLatitude() +" "+mLastLocation.getLongitude()));
                        final File videoSubtitles;
                        if (mIsInternal) {
                            videoSubtitles = new File(mVideoFolderInternal+"/"+mCommonName, mCommonName + ".txt");
                        } else {
                            videoSubtitles = new File(mVideoFolderExternal+"/"+mCommonName, mCommonName + ".txt");
                        }
                        try {
                            videoSubtitles.createNewFile();
                            FileOutputStream fileOutputStream = new FileOutputStream(videoSubtitles);
                            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                            for (String line : mSubtitles) {
                                outputStreamWriter.append(line);
                            }
                            outputStreamWriter.close();
                            fileOutputStream.flush();
                            fileOutputStream.close();
                            mSubtitles.clear();
                            Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            mediaStoreUpdateIntent.setData(Uri.fromFile(new File(videoSubtitles.getAbsolutePath())));
                            sendBroadcast(mediaStoreUpdateIntent);
                        } catch (Exception e) {
                            Log.e("Exception", "Nie udało sie zapisać napisów: " + e.toString());
                        }
                    }
                    mChronometer.stop();
                    mChronometer.setTextColor(Color.WHITE);
                    mIsRecording = false;
                    mRecordButton.setBackgroundResource(R.drawable.cast_ic_expanded_controller_play);
                    mRecordButton.setEnabled(false);
                    mSettingsButton.setEnabled(true);
                    stopRepeatingTask();
                    startPreview();
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                    Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaStoreUpdateIntent.setData(Uri.fromFile(new File(mVideoFileName)));
                    sendBroadcast(mediaStoreUpdateIntent);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    mRecordButton.setEnabled(true);
                }else{
                    Cursor data = mDatabaseHelper.getDataSettings();
                    Cursor dataStorage = mDatabaseHelper.getDataStorage();
                    data.moveToFirst();
                    dataStorage.moveToFirst();
                    long freeSpace;
                    File[] movieFile = ContextCompat.getExternalFilesDirs(CameraActivity.this, null);

                    if(isSDCardExists() || data.getString(5).toString().equals("Pamięć wewnętrzna")){
                        if(data.getString(5).toString().equals("Pamięć wewnętrzna")){
                            mFreeStorageSpace = movieFile[0];
                            freeSpace = movieFile[0].getFreeSpace() / (1024*1024);
                        }
                        else{
                            mFreeStorageSpace = movieFile[1];
                            freeSpace = movieFile[1].getFreeSpace() / (1024*1024);
                            dataStorage.moveToLast();
                        }
                        mLimitStorage = Long.valueOf(dataStorage.getString(1));
                        data.close();
                        dataStorage.close();
                        if(freeSpace <= mLimitStorage){
                            final AlertDialog.Builder dialog = new AlertDialog.Builder(CameraActivity.this);
                            dialog.setTitle("Uwaga");
                            dialog.setMessage("Pamięć urządzenia osiągnęła limit nagrywania.");
                            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            dialog.show();
                        }
                        else{
                            mSpeedButton.setText("0 Km/H");
                            mDistanceButton.setText("0,00 Km");
                            mDistance = (float)0;
                            if(!mGoogleApiClient.isConnected()){
                                mGoogleApiClient.connect();
                            }
                            checkWriteStoragePermission();
                            mSettingsButton.setEnabled(false);
                            mStartTime = System.currentTimeMillis();
                            startRepeatingTask();
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    }
                }
            }
        });
    }

    public void saveDefaultCameraSetup(){
        if(isEmptyTable(mDatabaseHelper.TABLE_SETTINGS)) {
            String resolution = String.valueOf(mVideoSize);
            String audio = "ON";
            String bitrate = String.valueOf(mVideoSize.getWidth() * mVideoSize.getHeight());
            String framerate = "30";
            String save = "Pamięć wewnętrzna";
            String storage = "OFF";
            mDatabaseHelper.addDataSettings(resolution, audio, bitrate, framerate, save, storage);
        }
    }

    public void saveDefaultStorageLimit(){
        if(isEmptyTable(mDatabaseHelper.TABLE_STORAGE)){
            mDatabaseHelper.addDataStorage("100");
            mDatabaseHelper.addDataStorage("100");
        }
    }

    public void saveResolutions(){
        if(isEmptyTable(mDatabaseHelper.TABLE_RESOLUTIONS)) {
            if(mCameraResolutions != null){
                for(Size size: mCameraResolutions){
                    mDatabaseHelper.addDataResolution(String.valueOf(size));
                }
            }
        }
    }

    public boolean isEmptyTable(String tableName) {
        boolean flag;
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM "+ tableName, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        if (count > 0) {
            flag =  false;
        } else {
            flag = true;
        }
        cursor.close();
        db.close();

        return flag;
    }

    private void setupCamera(int width, int height){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            for(String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mCameraResolutions = map.getOutputSizes(SurfaceTexture.class);
                saveResolutions();

                mPreviewSize = getOptimalPreviewSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mVideoSize = getOptimalPreviewSize(map.getOutputSizes(MediaRecorder.class), width, height);

                saveDefaultCameraSetup();
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            } else {
                Toast.makeText(this, "Wymagany dostęp do kamery.", Toast.LENGTH_SHORT).show();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startRecord(){
        try {
            setupMediaRecorder();
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            Surface recordSurface = mMediaRecorder.getSurface();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.addTarget(recordSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface,recordSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            try{
                                session.setRepeatingRequest(
                                        mCaptureRequestBuilder.build(), null, null
                                );
                            }catch(CameraAccessException e){
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {}
                    }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startPreview(){
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try{
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            try{
                                session.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                        null, mBackgroundHandler);
                            } catch(CameraAccessException e){
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Toast.makeText(getApplicationContext(),
                                    "Nie można ustawić obrazu kamery", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void closeCamera(){
        if(mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startBackgroundThread(){
        mBackgroundHandlerThread = new HandlerThread("Rejestrator");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread(){
        mBackgroundHandlerThread.quitSafely();
        try{
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Size getOptimalPreviewSize(Size[] sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)height / width;

        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        for (Size option : sizes) {
            double ratio = (double) option.getWidth() / option.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(option.getHeight() - targetHeight) < minDiff) {
                optimalSize = option;
                minDiff = Math.abs(option.getHeight() - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size option : sizes) {
                if (Math.abs(option.getHeight() - targetHeight) < minDiff) {
                    optimalSize = option;
                    minDiff = Math.abs(option.getHeight() - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private void transformImage(int width, int height){
        if(mPreviewSize == null || mTextureView == null) {
            return;
        }
        Matrix matrix = new Matrix();
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        RectF textureRectF = new RectF(0, 0, width, height);
        RectF previewRecF = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = textureRectF.centerX();
        float centerY = textureRectF.centerY();
        if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            previewRecF.offset(centerX - previewRecF.centerX(),
                    centerY - previewRecF.centerY());
            matrix.setRectToRect(textureRectF, previewRecF, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float)width / mPreviewSize.getWidth(),
                    (float)height / mPreviewSize.getHeight());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void createVideoFolder() {
        File[] movieFile = ContextCompat.getExternalFilesDirs(this, null);// 2 miejsca zapisu
        mVideoFolderInternal = new File(movieFile[0], "RejestratorVideos"); // internal
        if(!mVideoFolderInternal.exists()) {
            mVideoFolderInternal.mkdirs();
        }
        if(isSDCardExists()){
            mVideoFolderExternal = new File(movieFile[1], "RejestratorVideos"); // external
            if(!mVideoFolderExternal.exists()) {
                mVideoFolderExternal.mkdirs();
            }
        }
    }

    private File createVideoFileName() throws IOException {
        Cursor data = mDatabaseHelper.getDataSettings();
        data.moveToFirst();

        String recordTime = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        mCommonName = "VIDEO_" + recordTime;
        File videoFile = null;
        if(data.getString(5).toString().equals("Pamięć wewnętrzna")){
            mIsInternal = true;
            File videoFolder = new File(mVideoFolderInternal, mCommonName);
            if(!videoFolder.exists()) {
                videoFolder.mkdirs();
            }
            videoFile = new File(videoFolder, mCommonName + ".mp4");
        }
        else if(data.getString(5).toString().equals("Karta SD")){
            mIsInternal = false;
            File videoFolder = new File(mVideoFolderExternal, mCommonName);
            if(!videoFolder.exists()) {
                videoFolder.mkdirs();
            }
            videoFile = new File(videoFolder, mCommonName + ".mp4");
        }
        mVideoFileName = videoFile.getAbsolutePath();
        data.close();
        return videoFile;
    }

    private void checkWriteStoragePermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            mRecordButton.setEnabled(false);
            mIsRecording = true;
            mRecordButton.setBackgroundResource(R.drawable.cast_ic_expanded_controller_stop);
            try {
                createVideoFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }
            startRecord();
            mMediaRecorder.start();
            mChronometer.setBase(SystemClock.elapsedRealtime());
            mChronometer.setTextColor(Color.RED);
            mChronometer.start();
            mRecordButton.setEnabled(true);
        }else {
            Toast.makeText(this, "Potrzebne uprawnienia do zapisania nagrania", Toast.LENGTH_SHORT).show();

        }
    }

    private void setupMediaRecorder() throws IOException{
        Cursor dataSettings = mDatabaseHelper.getDataSettings();
        dataSettings.moveToFirst();

        Size videoSize = Size.parseSize(dataSettings.getString(1));

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        if(dataSettings.getString(2).toString().equals("ON")){
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mVideoFileName);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        if(dataSettings.getString(2).toString().equals("ON")){
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        }
        mMediaRecorder.setVideoEncodingBitRate(Integer.parseInt(dataSettings.getString(3)));
        mMediaRecorder.setVideoFrameRate(Integer.parseInt(dataSettings.getString(4)));
        mMediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
        dataSettings.close();
        mMediaRecorder.prepare();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CameraActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }
    @Override
    public void onConnectionSuspended(int i) {}
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if(mLastLocation == null){
                mLastLocation = location;
                mTimeOfLastLocation = System.currentTimeMillis();
            }
            else{
                mSubtitles.add(
                        String.valueOf(System.currentTimeMillis() - mStartTime) + "-" + // czas zakonczenia linijki tekstu
                        String.format("%.0f",mSpeed) + " Km/H" + " " + // prędkosc
                        String.format("%.2f",mDistance/(float)1000) + " Km" + " " + // dotychczasowy dystans
                        String.valueOf(mLastLocation.getLatitude() +" "+mLastLocation.getLongitude())+"\n"); // wspolrzedne
                float distance = mLastLocation.distanceTo(location);
                float seconds = (System.currentTimeMillis() - mTimeOfLastLocation) / (float)1000;
                mSpeed = (distance / (float)1000) / (seconds / 3600);
                mDistance += distance;

                mDistanceButton.setText(String.format("%.2f",mDistance/(float)1000) + " Km");
                mSpeedButton.setText(String.format("%.0f",mSpeed) + " Km/H");
                mTimeOfLastLocation = System.currentTimeMillis();
                mLastLocation = location;
            }
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.d(TAG, "Urządzenie nie wspiera GooglePlayServices.");
                finish();
            }
            return false;
        }
        Log.d(TAG, "Urządzenie wspiera GooglePlayServices.");
        return true;
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Uwaga!");
        dialog.setMessage("Opcja lokalizacji wyłączona.\nWłącz ją przechodząc do ustawień.");
        dialog.setPositiveButton("Ustawienia", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(myIntent);
            }
        });
        dialog.setNegativeButton("Anuluj", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
            }
        });
        dialog.show();
    }
}
