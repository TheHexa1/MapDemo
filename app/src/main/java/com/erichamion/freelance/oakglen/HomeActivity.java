package com.erichamion.freelance.oakglen;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.skobbler.ngx.SKPrepareMapTextureListener;
import com.skobbler.ngx.SKPrepareMapTextureThread;

import java.io.File;
import java.util.List;


public class HomeActivity extends MenuHandlerActivity implements SKPrepareMapTextureListener,
        BookContents.OnContentsAvailableListener  {

    private RelativeLayout mRootContentView;
    private Intent mMapIntent;
    private double mLatitude = 34.0525, mLongitude = -116.953889;
    private static final String PREFKEY_TITLE_VISITED = "titleVisited";
    private String mMapStorageDirName;

    int PERMISSION_ALL = 321;
    String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE};

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //************************************************
        //        ask for permissions in newer version of android.
        boolean flag = hasPermissions(this, PERMISSIONS);

        if(!flag){
//            permission_allowed = 0;
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        Glide.with(this).load(R.drawable.homescreen_bg).into((ImageView) findViewById(R.id.iv_home_screen_bg));

        mRootContentView = (RelativeLayout) findViewById(R.id.rootContentLayout);

        (findViewById(R.id.iv_gallery)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeActivity.this,MainActivity.class);
                startActivity(i);
            }
        });

        (findViewById(R.id.iv_map)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeActivity.this,MapWithPinsActivity.class);
                startActivity(i);
            }
        });

        (findViewById(R.id.iv_help)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuStartAboutActivity(AboutActivity.ABOUT_APP);
            }
        });

        (findViewById(R.id.iv_home_screen_bg)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchMap();
            }
        });

        final SharedPreferences prefs = getSharedPreferences(Util.SHARED_PREFERENCES_KEY, MODE_PRIVATE);

        if (!prefs.contains(Util.PREFKEY_EULA)) {
            enableMenu(false);
            final View eulaView = getLayoutInflater().inflate(R.layout.eula, mRootContentView, false);
            TextView tv_eula = (TextView) eulaView.findViewById(R.id.tv_eula);
            tv_eula.setMovementMethod(LinkMovementMethod.getInstance());
            View cancelView = eulaView.findViewById(R.id.cancel);
            cancelView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            View okView = eulaView.findViewById(R.id.ok);
            okView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mRootContentView.removeView(eulaView);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(Util.PREFKEY_EULA, true);
                    editor.apply();
                    enableMenu(true);

                    if (prefs.getBoolean(Util.PREFKEY_AUTOCONNECT, true)) {
                        Util.startWiFiService(HomeActivity.this);
                    }
                }
            });

            RelativeLayout.LayoutParams eulaParams =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mRootContentView.addView(eulaView, eulaParams);

        } else if (prefs.getBoolean(Util.PREFKEY_AUTOCONNECT, true)) {
            Util.startWiFiService(this);
        }

        mMapStorageDirName = prefs.getString(Util.PREFKEY_MAPRESOURCESPATH,
                getEffectiveFilesDir().getPath() + "/" + "SKMaps/");
        if (!new File(mMapStorageDirName).exists()) {
            new SKPrepareMapTextureThread(this, mMapStorageDirName, "SKMaps.zip", this).start();
        } else {
            initMapLibraryAndEnableClicks();
        }
        prefs.edit().putString(Util.PREFKEY_MAPRESOURCESPATH, mMapStorageDirName).apply();
    }

    //**************************
    public static boolean hasPermissions(Context context, String... permissions) {
        boolean hasAllPermissions = true;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            //for Android M and above versions
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    // Permission Denied
//                    Toast.makeText(context,"You need to allow all the required permissions to access this app!", Toast.LENGTH_LONG)
//                            .show();
                    hasAllPermissions = false;
                }
            }
        }
        return hasAllPermissions;
    }

//    public void initOnCreate(){
//
//    }

    @Override
    protected void onStart() {
        super.onStart();
        setUpContents();
    }

    @Override
    public void onMapTexturesPrepared(boolean b) {
        initMapLibraryAndEnableClicks();
    }

    private File getEffectiveFilesDir() {
        File filesDir = getExternalFilesDir(null);
        if (filesDir == null) {
            filesDir = getFilesDir();
        }
        return filesDir;
    }

    private void initMapLibraryAndEnableClicks() {
        Util.initMapLibrary(this, mMapStorageDirName);

//        mIsMapLibraryInitialized = true;
//        mRootContentView.removeView(mOverlayView);
//        mOverlayView = null;
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ActivityCompat.finishAffinity(this);
    }

    public void setNumVisitedLocations(int visitedCount) {
//        visitedLocationsIndicator.setRating(visitedLocations);
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

    @Override
    public void onContentsAvailable(BookContents contents) {
        //populate visited locations with red apple marker
        Pair<Integer, Integer> totalAndVisitedLocations = countVisitedLocations(contents);
        setNumVisitedLocations(totalAndVisitedLocations.second);
    }

    private void setUpContents() {
        BookContents.requestContents(this, this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        //******************************************************
        if(requestCode == PERMISSION_ALL){

            for(int i=0; i<grantResults.length; i++){
                if(grantResults[i] == PackageManager.PERMISSION_DENIED){
                    Toast.makeText(getApplicationContext(),"You need to allow all the required permissions to use this app!", Toast.LENGTH_LONG)
                            .show();
                    ActivityCompat.finishAffinity(this);
                }
            }
            return;
        }

    }
}
