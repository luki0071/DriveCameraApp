package com.example.kwasheniak.rejestratorjazdyandroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;


public class DatabaseHelper extends SQLiteOpenHelper{

    public static final String DATABASE_NAME = "CameraDB.db";

    public static final String TABLE_RESOLUTIONS = "RESOLUTIONS";
    public static final String COLUMN_ID_RESOLUTIONS = "ID";
    public static final String COLUMN_SIZE_RESOLUTIONS = "SIZE";
    private static final String CREATE_TABLE_RESOLUTIONS =
            "CREATE TABLE " + TABLE_RESOLUTIONS + " ( " +
                    COLUMN_ID_RESOLUTIONS + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_SIZE_RESOLUTIONS + " TEXT)";

    public static final String TABLE_SETTINGS = "SETTINGS";
    public static final String COLUMN_ID_SETTINGS = "ID";
    public static final String COLUMN_RESOLUTION_SETTINGS = "RESOLUTION";
    public static final String COLUMN_AUDIO_SETTINGS = "AUDIO";
    public static final String COLUMN_BITRATE_SETTINGS = "BITRATE";
    public static final String COLUMN_FRAMERATE_SETTINGS = "FRAMERATE";
    public static final String COLUMN_SAVELOCATION_SETTINGS = "SAVELOCATION";
    public static final String COLUMN_FREESPACE_SETTINGS = "FREESPACE";
    private static final String CREATE_TABLE_SETTINGS =
            "CREATE TABLE " + TABLE_SETTINGS + " ( " +
                    COLUMN_ID_SETTINGS + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_RESOLUTION_SETTINGS + " TEXT, " +
                    COLUMN_AUDIO_SETTINGS + " TEXT, " +
                    COLUMN_BITRATE_SETTINGS + " TEXT, " +
                    COLUMN_FRAMERATE_SETTINGS + " TEXT, " +
                    COLUMN_SAVELOCATION_SETTINGS + " TEXT, " +
                    COLUMN_FREESPACE_SETTINGS + " TEXT)";

    public static final String TABLE_STORAGE = "STORAGE";
    public static final String COLUMN_ID_STORAGE = "ID";
    public static final String COLUMN_LIMIT_STORAGE = "SIZE";
    private static final String CREATE_TABLE_STORAGE =
            "CREATE TABLE " + TABLE_STORAGE + " ( " +
                    COLUMN_ID_STORAGE + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_LIMIT_STORAGE + " TEXT)";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_RESOLUTIONS);
        db.execSQL(CREATE_TABLE_SETTINGS);
        db.execSQL(CREATE_TABLE_STORAGE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP IF TABLE EXISTS " + TABLE_RESOLUTIONS);
        db.execSQL("DROP IF TABLE EXISTS " + TABLE_SETTINGS);
        db.execSQL("DROP IF TABLE EXISTS " + TABLE_STORAGE);
        onCreate(db);
    }

    public boolean addDataResolution(String item){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_SIZE_RESOLUTIONS, item);

        long result = db.insert(TABLE_RESOLUTIONS, null, contentValues);

        if(result == -1){return false;}
        else{return true;}
    }

    public Cursor getDataResolution(){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_RESOLUTIONS;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public boolean addDataSettings(String resolution, String audio, String bitrate, String framerate, String save, String storage){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_RESOLUTION_SETTINGS, resolution);
        contentValues.put(COLUMN_AUDIO_SETTINGS, audio);
        contentValues.put(COLUMN_BITRATE_SETTINGS, bitrate);
        contentValues.put(COLUMN_FRAMERATE_SETTINGS, framerate);
        contentValues.put(COLUMN_SAVELOCATION_SETTINGS, save);
        contentValues.put(COLUMN_FREESPACE_SETTINGS, storage);

        long result = db.insert(TABLE_SETTINGS, null, contentValues);

        if(result == -1){return false;}
        else{return true;}
    }

    public Cursor getDataSettings(){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_SETTINGS;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public boolean addDataStorage(String size){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_LIMIT_STORAGE, size);

        long result = db.insert(TABLE_STORAGE, null, contentValues);

        if(result == -1){return false;}
        else{return true;}
    }

    public Cursor getDataStorage(){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_STORAGE;
        Cursor data = db.rawQuery(query, null);
        return data;
    }
}
