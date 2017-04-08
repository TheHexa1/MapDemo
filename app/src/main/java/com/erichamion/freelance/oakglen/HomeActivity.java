package com.erichamion.freelance.oakglen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
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

import com.bumptech.glide.Glide;
import com.skobbler.ngx.SKPrepareMapTextureListener;
import com.skobbler.ngx.SKPrepareMapTextureThread;

import java.io.File;
import java.util.List;


public class HomeActivity extends MenuHandlerActivity implements SKPrepareMapTextureListener {

    private RelativeLayout mRootContentView;
    private Intent mMapIntent;
    private double mLatitude = 34.0525, mLongitude = -116.953889;
    private static final String PREFKEY_TITLE_VISITED = "titleVisited";
    private String mMapStorageDirName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Glide.with(this).load(R.drawable.homescreen_bg).into((ImageView) findViewById(R.id.iv_home_screen_bg));

        mRootContentView = (RelativeLayout) findViewById(R.id.rootContentLayout);

        (findViewById(R.id.iv_gallery)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeActivity.this,MainActivity.class);
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

//        SpannableString spannableString = new SpannableString("Lorem     ");
//        Drawable d = getResources().getDrawable(R.drawable.map);
//        d.setBounds(0, 0, 50, 50);
//        ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
////        ImageSpan span = new ImageSpan(this,R.drawable.map);
//        spannableString.setSpan(span, 5,  5+1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
//        ((TextView)findViewById(R.id.tv_test)).setText(spannableString);

        prefs.edit().putString(Util.PREFKEY_MAPRESOURCESPATH, mMapStorageDirName).apply();

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
}
