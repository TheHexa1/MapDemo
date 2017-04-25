package com.erichamion.freelance.oakglen;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.AppCompatRatingBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.erichamion.freelance.oakglen.rcview.ContentsAdapter;
import com.skobbler.ngx.SKPrepareMapTextureListener;
import com.skobbler.ngx.SKPrepareMapTextureThread;

import java.io.File;
import java.util.List;

public class MainActivity
        extends MenuHandlerActivity
        implements BookContents.OnContentsAvailableListener {

    private static final String PREFKEY_TITLE_VISITED = "titleVisited";

    private String mMapStorageDirName;
    private Double mLatitude;
    private Double mLongitude;
    private RelativeLayout mRootContentView;
    private ViewGroup mTitleViewHolder;
    private FrameLayout mOverlayView;
    private ContentsAdapter mChapterHolderViewAdapter;

    private boolean mIsMapLibraryInitialized = false;
    private boolean mHasContents = false;
    private Intent mMapIntent;

    public Pair<Integer, Integer> countVisitedLocations(BookContents contents) {
        // Start with 1 to account for the title page
        int numLocations = 1;
        int visitedLocations = 0;
        SharedPreferences prefs = getSharedPreferences(Util.SHARED_PREFERENCES_KEY, MODE_PRIVATE);
        if (prefs.getBoolean(PREFKEY_TITLE_VISITED, false)) visitedLocations++;
        for (int chapterIndex = 0; chapterIndex < contents.getNumChapters(); chapterIndex++) {
            BookContents.Chapter currentChapter = contents.getChapter(chapterIndex);
            for (int pageIndex = 0; pageIndex < currentChapter.getNumPages(); pageIndex++) {
                numLocations++;
                String prefkey = Util.getVisitedPrefkey(chapterIndex, pageIndex);
                if(prefs.getBoolean(prefkey, false)) {
                    visitedLocations++;
                }
            }
        }
        return new Pair<>(numLocations, visitedLocations);


    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean canClick() {
        return mIsMapLibraryInitialized && mHasContents;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRootContentView = (RelativeLayout) findViewById(R.id.rootContentLayout);
        assert mRootContentView != null;

        mOverlayView = new FrameLayout(this);
        mOverlayView.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimaryDarkTransparent, getTheme()));
        RelativeLayout.LayoutParams overlayParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        overlayParams.alignWithParent = true;
        ProgressBar progressBar = new ProgressBar(this);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.thumbnail_height), getResources().getDimensionPixelSize(R.dimen.thumbnail_height), Gravity.CENTER);
        mOverlayView.addView(progressBar, progressParams);
        mRootContentView.addView(mOverlayView, overlayParams);

        TextView dummy = new TextView(this);
        dummy.setTextSize(TypedValue.COMPLEX_UNIT_FRACTION, .1f);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Don't just use long/notlong resources, because that
        // has an inflexible cutoff point that may not work
        // well for our needs.
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float ratio = (float) metrics.widthPixels / metrics.heightPixels;
        float maxRatio = Float.parseFloat(getResources().getString(R.string.shortlong_cutoff_ratio));
        boolean includeTitleInScroll;
        if (ratio <= maxRatio) {
            getLayoutInflater().inflate(R.layout.content_main, mRootContentView);
            mTitleViewHolder = (ViewGroup) findViewById(R.id.titleHolder);
            includeTitleInScroll = false;

            mTitleViewHolder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!canClick()) return;

                    launchMap();
                }
            });
        } else {
            getLayoutInflater().inflate(R.layout.content_main_short, mRootContentView);
            includeTitleInScroll = true;
        }

        RecyclerView chapterHolderView = (RecyclerView) findViewById(R.id.chaptersHolder);
        assert chapterHolderView != null;
        chapterHolderView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return super.getExtraLayoutSpace(state) / 3;
            }
        });
        mChapterHolderViewAdapter = new ContentsAdapter(this, includeTitleInScroll,
                (int) getResources().getDimension(R.dimen.thumbnail_width),
                (int) getResources().getDimension(R.dimen.thumbnail_height));
        chapterHolderView.setAdapter(mChapterHolderViewAdapter);

//        final SharedPreferences prefs = getSharedPreferences(Util.SHARED_PREFERENCES_KEY, MODE_PRIVATE);
//
//        mMapStorageDirName = prefs.getString(Util.PREFKEY_MAPRESOURCESPATH,
//                getEffectiveFilesDir().getPath() + "/" + "SKMaps/");
//        if (!new File(mMapStorageDirName).exists()) {
//            new SKPrepareMapTextureThread(this, mMapStorageDirName, "SKMaps.zip", this).start();
//        } else {
//            initMapLibraryAndEnableClicks();
//        }



//        if (!prefs.contains(Util.PREFKEY_EULA)) {
//            enableMenu(false);
//            final View eulaView = getLayoutInflater().inflate(R.layout.eula, mRootContentView, false);
//            View cancelView = eulaView.findViewById(R.id.cancel);
//            cancelView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    finish();
//                }
//            });
//            View okView = eulaView.findViewById(R.id.ok);
//            okView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    mRootContentView.removeView(eulaView);
//                    SharedPreferences.Editor editor = prefs.edit();
//                    editor.putBoolean(Util.PREFKEY_EULA, true);
//                    editor.apply();
//                    enableMenu(true);
//
//                    if (prefs.getBoolean(Util.PREFKEY_AUTOCONNECT, true)) {
//                        Util.startWiFiService(MainActivity.this);
//                    }
//                }
//            });
//
//            RelativeLayout.LayoutParams eulaParams =
//                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//            mRootContentView.addView(eulaView, eulaParams);
//
//        } else if (prefs.getBoolean(Util.PREFKEY_AUTOCONNECT, true)) {
//            Util.startWiFiService(this);
//        }

//        prefs.edit().putString(Util.PREFKEY_MAPRESOURCESPATH, mMapStorageDirName).apply();
        initMapLibraryAndEnableClicks();
    }

    private void setUpContents() {
        BookContents.requestContents(this, this);
    }

    @Override
    protected void onStart() {
        setUpContents();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mChapterHolderViewAdapter.setContents(null);
        super.onStop();
    }

//    @Override
//    public void onMapTexturesPrepared(boolean b) {
//        initMapLibraryAndEnableClicks();
//    }

    @Override
    public void onContentsAvailable(BookContents contents) {
        mLatitude = contents.getLatitude();
        mLongitude = contents.getLongitude();
        mHasContents = true;

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(contents.getTitle());

        int backgroundImageId = contents.getBackgroundResId();
        if (backgroundImageId != 0) {
            setMainBackground(mRootContentView, backgroundImageId, contents.getBackgroundGravity());
        }

        if (mTitleViewHolder != null) {
            TextView latTextView = (TextView) findViewById(R.id.latText);
            TextView longTextView = (TextView) findViewById(R.id.longText);
            Pair<String, String> latLong = Util.getCoordinateStrings(mLatitude, mLongitude);
            assert latTextView != null;
            latTextView.setText(latLong.first);
            assert longTextView != null;
            longTextView.setText(latLong.second);
            Pair<Integer, Integer> totalAndVisitedLocations = this.countVisitedLocations(contents);
            displayVisitedLocations(totalAndVisitedLocations.second);
//            AppCompatRatingBar visitedLocationsIndicator = (AppCompatRatingBar) mTitleViewHolder.findViewById(R.id.visitedLocationsIndicator);
//            assert visitedLocationsIndicator != null;
//            visitedLocationsIndicator.setNumStars(totalAndVisitedLocations.first);
//            visitedLocationsIndicator.setRating(totalAndVisitedLocations.second);
//            Util.setRatingBarToAccentColor(visitedLocationsIndicator, getTheme());

        }



        mChapterHolderViewAdapter.setContents(contents);
    }

//    private File getEffectiveFilesDir() {
//        File filesDir = getExternalFilesDir(null);
//        if (filesDir == null) {
//            filesDir = getFilesDir();
//        }
//        return filesDir;
//    }

    private void initMapLibraryAndEnableClicks() {
//        Util.initMapLibrary(this, mMapStorageDirName);

        mIsMapLibraryInitialized = true;
        mRootContentView.removeView(mOverlayView);
        mOverlayView = null;
        mChapterHolderViewAdapter.enableClicks();
    }

    public void launchMap() {
        // If permissions are already granted, map will launch.
        // Otherwise, use the stored mMapIntent to launch from the onRequestPermissionsResult
        // callback.
        mMapIntent = MapActivity.launchMap(this, mLatitude, mLongitude, PREFKEY_TITLE_VISITED, R.drawable.cover_image_thumbnail, getString(R.string.app_name));
    }

    @Override
    public boolean onRequestPermissionsResultEx(int requestCode, @NonNull List<String> deniedPermissions) {
        if (requestCode == MapActivity.MAP_PERMS_REQUEST && mMapIntent != null) {
            if (deniedPermissions.isEmpty()) {
                startActivity(mMapIntent);
                mMapIntent = null;
            }

            return true;
        }

        return false;

    }

    public void displayVisitedLocations(int visitedCount){

        int indicator_apples[] = {
                R.id.indicator_apple_1,R.id.indicator_apple_2,R.id.indicator_apple_3,
                R.id.indicator_apple_4,R.id.indicator_apple_5,R.id.indicator_apple_6,
                R.id.indicator_apple_7,R.id.indicator_apple_8,R.id.indicator_apple_9,
                R.id.indicator_apple_10,R.id.indicator_apple_11,R.id.indicator_apple_12,
                R.id.indicator_apple_13,R.id.indicator_apple_14,R.id.indicator_apple_15,
        };

        for(int i=0; i<visitedCount; i++){
            ((ImageView)findViewById(indicator_apples[i])).setImageResource(R.drawable.colored_apple);
        }
    }

}
