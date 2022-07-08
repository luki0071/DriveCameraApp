package com.example.kwasheniak.rejestratorjazdyandroid;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class DirectoryFragment extends Fragment {

    private ListView mListView;
    private ArrayList<String> mArrayList;
    private File mDirectory;
    private TextView mTextViewInfo;

    public DirectoryFragment() {}

    public static final DirectoryFragment newInstance(String file) { // konstruktor z argumentem pobiera scieżke do folderu

        DirectoryFragment fragment = new DirectoryFragment();
        final Bundle args = new Bundle();
        args.putString("directory_path", file);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_directory, container, false);
        mListView = (ListView) view.findViewById(R.id.listViewDirectory);
        mTextViewInfo = (TextView) view.findViewById(R.id.textViewInfo);
        setListView();

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { // po długim kliknieciu na pozycje z listy pokazuje opcie usuniecia
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                final String listItemName = mListView.getItemAtPosition(position).toString();
                final PopupMenu popup = new PopupMenu(getActivity(), view);
                popup.getMenu().add("Usuń");
                popup.getMenuInflater().inflate(R.menu.menu_resolution, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        File videoFile = new File(mDirectory, listItemName);
                        deleteFile(videoFile);
                        Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        mediaStoreUpdateIntent.setData(Uri.fromFile(new File(videoFile.getAbsolutePath())));
                        getActivity().sendBroadcast(mediaStoreUpdateIntent);
                        setListView();
                        return true;
                    }
                });
                popup.show();
                return true;
            }
        });
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // po kliknięciu przenosi do odtwazracza i zapisuje informacje o folderze i sciezce nagrania
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String listItemName = mListView.getItemAtPosition(position).toString();
                File videoFile = new File(mDirectory, listItemName);
                if(videoFile.exists()){
                    Intent intent = new Intent(getActivity(), MediaPlayerActivity.class);
                    intent.putExtra("DIRECTORY_VIDEO_PATH", videoFile.getAbsolutePath());
                    intent.putExtra("FILE_NAME", listItemName);
                    startActivity(intent);
                }
                else{
                    final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                    dialog.setTitle("Uwaga!");
                    dialog.setMessage("Nagranie nie istnieje lub nie ma dostępu do pamięci.\nSprawdź kartę sd.");
                    dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                    setListView();
                }
            }
        });
        return view;
    }

    public void deleteFile(File file){ // usuwa folder z nagraniem wraz z plikami które sie w nim znajdują
        if(file.isDirectory()) {
            File[] files = file.listFiles();
            if(files.length > 0){
                for (File video : files) {
                    deleteFile(video);
                }
            }
        }
        file.delete();
    }

    public void setListView(){ // ustawia liste nagran z folder
        try{
            mListView.setVisibility(View.VISIBLE);
            mTextViewInfo.setVisibility(View.GONE);
            mArrayList = new ArrayList<String>();
            mDirectory = new File(getArguments().getString("directory_path"),"RejestratorVideos");
            if(mDirectory.isDirectory()){
                File[] files = mDirectory.listFiles();
                if(files.length > 0){
                    for(File video: files){
                        mArrayList.add(video.getName());
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                            getActivity(), android.R.layout.simple_expandable_list_item_1, mArrayList);
                    mListView.setAdapter(arrayAdapter);
                }
                else{
                    mListView.setVisibility(View.GONE);
                    mTextViewInfo.setVisibility(View.VISIBLE);
                    mTextViewInfo.setText("Folder jest pusty");
                }
            }
            else{
                mListView.setVisibility(View.GONE);
                mTextViewInfo.setVisibility(View.VISIBLE);
                mTextViewInfo.setText("Folder nie istnieje");
            }
        }catch (Exception e){
            mListView.setVisibility(View.GONE);
            mTextViewInfo.setVisibility(View.VISIBLE);
            mTextViewInfo.setText("Pamięć nie dostępna");
        }

    }
}
