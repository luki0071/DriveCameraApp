package com.example.kwasheniak.rejestratorjazdyandroid;


import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import java.io.File;

public class RecordedVideosActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private ViewPagerAdapter mViewPagerAdapter;
    private File[] mDirectories;

    @Override
    protected void onCreate(Bundle savedInstanceState) { // ustawia widok dla obsługi folderów z nagraniami
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorded_videos);

        mToolbar = (Toolbar) findViewById(R.id.activity_toolbar);
        setSupportActionBar(mToolbar);

        mDirectories = ContextCompat.getExternalFilesDirs(this, null);

        mTabLayout = (TabLayout) findViewById(R.id.tabLayout);
        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        mViewPagerAdapter.addFragments(new DirectoryFragment().newInstance(String.valueOf(mDirectories[0])), "Pamięć wewnętrzna");
        mViewPagerAdapter.addFragments(new DirectoryFragment().newInstance(String.valueOf(mDirectories[1])), "Karta SD");
        mViewPager.setAdapter(mViewPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
    }
}
