package com.example.kwasheniak.rejestratorjazdyandroid;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.util.Size;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity{

    private static final String TAG = "MyActivity";

    private DatabaseHelper mDatabaseHelper;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        mDatabaseHelper = new DatabaseHelper(this);
        listView = (ListView) findViewById(R.id.listParams);

        setListView();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Cursor data = mDatabaseHelper.getDataSettings();
                data.moveToFirst();
                PopupMenu popup = new PopupMenu(SettingsActivity.this, view);
                switch((int)id){
                    case 0:
                        Cursor cursor = mDatabaseHelper.getDataResolution();
                        if(cursor.getCount() != 0){
                            while(cursor.moveToNext()){
                                popup.getMenu().add(cursor.getString(1));
                            }
                        }
                        popup.getMenuInflater().inflate(R.menu.menu_resolution, popup.getMenu());
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {

                                SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(mDatabaseHelper.COLUMN_RESOLUTION_SETTINGS, item.toString());
                                Size size = Size.parseSize(item.toString());
                                contentValues.put(mDatabaseHelper.COLUMN_BITRATE_SETTINGS, String.valueOf(size.getWidth()*size.getHeight()));

                                db.update(mDatabaseHelper.TABLE_SETTINGS, contentValues, "ID = 1", null);
                                db.close();
                                setListView();

                                return true;
                            }
                        });
                        popup.show();
                        break;
                    case 1:
                        popup.getMenu().add("ON");
                        popup.getMenu().add("OFF");
                        popup.getMenuInflater().inflate(R.menu.menu_resolution, popup.getMenu());
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {

                                SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(mDatabaseHelper.COLUMN_AUDIO_SETTINGS, item.toString());

                                db.update(mDatabaseHelper.TABLE_SETTINGS, contentValues, "ID = 1", null);
                                db.close();
                                setListView();

                                return true;
                            }
                        });
                        popup.show();
                        break;
                    case 2:
                        final View bitrateView = getLayoutInflater().inflate(R.layout.activity_bitrate, null);
                        final AlertDialog.Builder bitrateBuilder = new AlertDialog.Builder(SettingsActivity.this);
                        final EditText inputBitrate = (EditText) bitrateView.findViewById(R.id.editTextBitrate);
                        Button buttonAcceptBitrate = (Button) bitrateView.findViewById(R.id.buttonAccept);
                        Button buttonInfo = (Button) bitrateView.findViewById(R.id.buttonInfo);

                        bitrateBuilder.setView(bitrateView);
                        final AlertDialog dialogBitrate = bitrateBuilder.create();

                        buttonInfo.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String info = "Im większa wartość bitrate'u tym lepsza jakość nagrania " +
                                        "(wyraźniejszy, ostrzejszy obraz). Dozwolona wartość musi mieścić się w " +
                                        "przedziale 10000 - 10000000";
                                final AlertDialog.Builder mBuilder = new AlertDialog.Builder(SettingsActivity.this);
                                mBuilder.setTitle("Bitrate");
                                mBuilder.setMessage(info);
                                mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                AlertDialog alertDialog = mBuilder.create();
                                alertDialog.show();
                            }
                        });

                        buttonAcceptBitrate.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(inputBitrate.getText().toString().isEmpty()){
                                    Toast.makeText(SettingsActivity.this, "Podaj wartość bitrate'u", Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    if(Integer.parseInt(inputBitrate.getText().toString()) >= 10000 &&
                                            Integer.parseInt(inputBitrate.getText().toString()) <= 10000000){

                                        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                                        ContentValues contentValues = new ContentValues();
                                        contentValues.put(mDatabaseHelper.COLUMN_BITRATE_SETTINGS, inputBitrate.getText().toString());

                                        db.update(mDatabaseHelper.TABLE_SETTINGS, contentValues, "ID = 1", null);
                                        db.close();
                                        setListView();

                                        dialogBitrate.dismiss();
                                    }
                                    else{
                                        Toast.makeText(SettingsActivity.this, "Podana wartość nie mieści sie w przedziale 10000 - 10000000", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
                        dialogBitrate.show();
                        break;
                    case 3:
                        popup.getMenu().add("30");
                        popup.getMenu().add("60");
                        popup.getMenu().add("120");
                        popup.getMenuInflater().inflate(R.menu.menu_resolution, popup.getMenu());
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {

                                SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(mDatabaseHelper.COLUMN_FRAMERATE_SETTINGS, item.toString());

                                db.update(mDatabaseHelper.TABLE_SETTINGS, contentValues, "ID = 1", null);
                                db.close();
                                setListView();

                                return true;
                            }
                        });
                        popup.show();
                        break;
                    case 4:
                        popup.getMenu().add("Pamięć wewnętrzna");
                        try{
                            ContextCompat.getExternalFilesDirs(SettingsActivity.this, null)[1].toString();
                            popup.getMenu().add("Karta SD");
                        }catch(Exception e){
                            if(data.getString(5).toString().equals("Karta SD")){
                                SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(mDatabaseHelper.COLUMN_SAVELOCATION_SETTINGS, "Pamięć wewnętrzna");

                                db.update(mDatabaseHelper.TABLE_SETTINGS, contentValues, "ID = 1", null);
                                db.close();
                                setListView();
                            }
                        }
                        popup.getMenuInflater().inflate(R.menu.menu_resolution, popup.getMenu());
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {

                                SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(mDatabaseHelper.COLUMN_SAVELOCATION_SETTINGS, item.toString());

                                db.update(mDatabaseHelper.TABLE_SETTINGS, contentValues, "ID = 1", null);
                                setListView();

                                return true;
                            }
                        });
                        popup.show();
                        break;
                    case 5:
                        popup.getMenu().add("ON");
                        popup.getMenu().add("OFF");
                        popup.getMenuInflater().inflate(R.menu.menu_resolution, popup.getMenu());
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {

                                SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(mDatabaseHelper.COLUMN_FREESPACE_SETTINGS, item.toString());

                                db.update(mDatabaseHelper.TABLE_SETTINGS, contentValues, "ID = 1", null);
                                db.close();
                                setListView();

                                return true;
                            }
                        });
                        popup.show();
                        break;
                    case 6:
                        final View limitView = getLayoutInflater().inflate(R.layout.activity_storage, null);
                        final AlertDialog.Builder limitBuilder = new AlertDialog.Builder(SettingsActivity.this);
                        final EditText inputLimit = (EditText) limitView.findViewById(R.id.editTextLimit);

                        Button buttonAcceptStorage = (Button) limitView.findViewById(R.id.buttonAcceptStorage);
                        File[] movieFile = ContextCompat.getExternalFilesDirs(SettingsActivity.this, null);
                        final long valueInMB;
                        final int ID;
                        if(data.getString(5).toString().equals("Pamięć wewnętrzna")){
                            valueInMB = movieFile[0].getFreeSpace() / (1024*1024);
                            ID = 1;
                        }
                        else if(data.getString(5).toString().equals("Karta SD")){
                            valueInMB = movieFile[1].getFreeSpace() / (1024*1024);
                            ID = 2;
                        }
                        else{
                            valueInMB = 0;
                            ID = 0;
                        }
                        limitBuilder.setView(limitView);
                        final AlertDialog dialogLimit = limitBuilder.create();

                        buttonAcceptStorage.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(inputLimit.getText().toString().isEmpty()){
                                    Toast.makeText(SettingsActivity.this, "Podaj wartość limitu", Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    if(Integer.parseInt(inputLimit.getText().toString()) >= 100 &&
                                            Integer.parseInt(inputLimit.getText().toString()) <= valueInMB){

                                        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
                                        ContentValues contentValues = new ContentValues();
                                        contentValues.put(mDatabaseHelper.COLUMN_LIMIT_STORAGE, inputLimit.getText().toString());

                                        db.update(mDatabaseHelper.TABLE_STORAGE, contentValues, "ID = " + ID, null);
                                        db.close();
                                        setListView();

                                        dialogLimit.dismiss();
                                    }
                                    else{
                                        Toast.makeText(SettingsActivity.this, "Podana wartość nie mieści sie w przedziale 100 MB - " + String.valueOf(valueInMB) + " MB", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
                        dialogLimit.show();
                        break;
                    default:
                        Log.i(TAG,"nothing");
                }
            }
        });
    }

    public void setListView(){
        final List<String[]> settingList = new LinkedList<String[]>();
        Cursor dataSettings = mDatabaseHelper.getDataSettings();
        dataSettings.moveToFirst();

        int i = 1;
        String optionName = null;

        do{
            switch(i){
                case 1:
                    optionName = "Rozdzielczość";
                    break;
                case 2:
                    optionName = "Audio";
                    break;
                case 3:
                    optionName = "Bitrate";
                    break;
                case 4:
                    optionName = "Ilość klatek";
                    break;
                case 5:
                    optionName = "Miejsce zapisu";
                    break;
                case 6:
                    optionName = "Pokaż wolną pamięć w czasie nagrywania";
                    break;
                default:
                    Log.i(TAG,"nothing");
            }
            settingList.add(new String[] {optionName ,dataSettings.getString(i)});
            i++;
        }while(i < dataSettings.getColumnCount());

        Cursor dataStorage = mDatabaseHelper.getDataStorage();

        String limit = "0";

        if(dataSettings.getString(5).toString().equals("Pamięć wewnętrzna")){
            dataStorage.moveToFirst();
            limit = dataStorage.getString(1).toString();
        }
        else if(dataSettings.getString(5).toString().equals("Karta SD")){
            dataStorage.moveToLast();
            limit = dataStorage.getString(1).toString();
        }

        settingList.add(new String[]{"Limit nagrywania", limit + " MB"});

        ArrayAdapter<String[]> adapter = new ArrayAdapter<String[]>(this, android.R.layout.simple_list_item_2, android.R.id.text1, settingList){

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                String[] entry = settingList.get(position);
                TextView textColumnName = (TextView) view.findViewById(android.R.id.text1);
                TextView textOption = (TextView) view.findViewById(android.R.id.text2);
                textColumnName.setText(entry[0]);
                textOption.setText(entry[1]);
                return view;
            }
        };
        listView.setAdapter(adapter);
    }
}
