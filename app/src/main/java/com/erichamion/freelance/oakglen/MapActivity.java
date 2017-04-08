package com.erichamion.freelance.oakglen;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.erichamion.freelance.oakglen.bm.BitmapManager;
import com.erichamion.freelance.oakglen.map.MapHandler;
import com.erichamion.freelance.oakglen.map.MapUiHandler;
import com.skobbler.ngx.map.SKMapViewHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapActivity extends MenuHandlerActivity implements MapUiHandler {
    private MapHandler mMapHandler;

    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_THUMB = "thumbnail";
    private static final String KEY_TITLE = "title";
    private static final String KEY_VISITED_KEY = "visitedPrefkey";
    public static final int MAP_PERMS_REQUEST = R.id.permissionRequestMap & ((1 << 16) - 1);

    Map<String, String> valid_qrcodes_map;
    private String pop_up_msg = "";

    private String mVisitedPrefKey;
    private boolean mHasBeenVisited;
    @DrawableRes private int mThumbnailId;
    private View mWaitingIndicator;
    private ImageView mAdviceImageView;
    private TextView mAdviceTextView;
    private ViewGroup mAdviceHolder;
    private SKMapViewHolder mMapView;
    private FloatingActionButton mFab;
    private FloatingActionButton mWalkButton;
    private TextToSpeech mTts;
    private BitmapManager.ImageLoaderHandle mAdviceImageLoaderHandle;
    private String mAdviceImagePath;
    private @DrawableRes int mAdviceImageResId;

    @Nullable
    public static Intent launchMap(AppCompatActivity fromActivity, double latitude, double longitude, @NonNull String visitedPrefkey, @DrawableRes int thumbnailResId, String title) {
        Intent intent = new Intent(fromActivity, MapActivity.class);
        intent.putExtra(KEY_LATITUDE, latitude);
        intent.putExtra(KEY_LONGITUDE, longitude);
        intent.putExtra(KEY_THUMB, thumbnailResId);
        intent.putExtra(KEY_TITLE, title);
        intent.putExtra(KEY_VISITED_KEY, visitedPrefkey);

        List<String> missingPerms = Util.getMissingPermissions(fromActivity, Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.INTERNET, Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE);

        if (missingPerms.isEmpty()) {
            fromActivity.startActivity(intent);
            return null;
        } else {
            ActivityCompat.requestPermissions(fromActivity, missingPerms.toArray(new String[missingPerms.size()]), MAP_PERMS_REQUEST);
            return intent;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mMapHandler = new MapHandler(this, this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mWalkButton = (FloatingActionButton) findViewById(R.id.walkFab);

        Bundle extras = getIntent().getExtras();
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(extras.getString(KEY_TITLE, "Untitled Map"));
        mVisitedPrefKey = extras.getString(KEY_VISITED_KEY);
        mHasBeenVisited = getSharedPreferences(Util.SHARED_PREFERENCES_KEY, MODE_PRIVATE)
                .getBoolean(mVisitedPrefKey, false);
        mThumbnailId = extras.getInt(KEY_THUMB);
        ImageView thumbView = (ImageView) findViewById(R.id.thumbnailImage);
        assert thumbView != null;
        BitmapManager.setImageViewBitmap(thumbView, mThumbnailId, Util.IMAGE_TYPE_THUMBNAIL,
                (int) getResources().getDimension(R.dimen.thumbnail_width),
                (int) getResources().getDimension(R.dimen.thumbnail_height),
                null);
        thumbView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mAdviceHolder = (ViewGroup) findViewById(R.id.adviceHolder);
        assert mAdviceHolder != null;
        mAdviceImageView = (ImageView) mAdviceHolder.findViewById(R.id.adviceImage);
        mAdviceTextView = (TextView) mAdviceHolder.findViewById(R.id.adviceText);
        assert mAdviceTextView != null;
        mAdviceTextView.setText(R.string.route_loading);
        mWaitingIndicator = mAdviceHolder.findViewById(R.id.waitingIndicator);

        double densityRatio = (double) getResources().getDisplayMetrics().densityDpi / (double) DisplayMetrics.DENSITY_MEDIUM;
        mMapView = (SKMapViewHolder) findViewById(R.id.view_group_map);
        assert mMapView != null;
        mMapHandler.init(densityRatio, extras.getDouble(KEY_LATITUDE), extras.getDouble(KEY_LONGITUDE), !mHasBeenVisited, mMapView);

        //for pop-up text
        try {

            valid_qrcodes_map = new HashMap();

            //        try { total 35 gps locations>>
//            valid_qrcodes_map.put("{\"lat\":\"21.331767\",\"long\":\"-158.120217\"}","aloha");
//            valid_qrcodes_map.put("{\"lat\":\"21.331317\",\"long\":\"-158.120217\"}","ana_puka");
//            valid_qrcodes_map.put("{\"lat\":\"21.33085\",\"long\":\"-158.1224\"}","hao_hakahaka");
//            valid_qrcodes_map.put("{\"lat\":\"21.332167\",\"long\":\"-158.12145\"}","honu");
//            valid_qrcodes_map.put("{\"lat\":\"21.33325\",\"long\":\"-158.121083\"}","hooikaika_kino");
//            valid_qrcodes_map.put("{\"lat\":\"21.332483\",\"long\":\"-158.1208\"}","i_a");
//            valid_qrcodes_map.put("{\"lat\":\"21.331417\",\"long\":\"-158.120883\"}","la_au");
//            valid_qrcodes_map.put("{\"lat\":\"21.332517\",\"long\":\"-158.121617\"}","lahalaha_wai");
//            valid_qrcodes_map.put("{\"lat\":\"21.3326\",\"long\":\"-158.122083\"}","lomi");
//            valid_qrcodes_map.put("{\"lat\":\"21.33215\",\"long\":\"-158.120883\"}","mea_pani");
//            valid_qrcodes_map.put("{\"lat\":\"21.330667\",\"long\":\"-158.1223\"}","pahu");
//            valid_qrcodes_map.put("{\"lat\":\"21.332133\",\"long\":\"-158.121833\"}","papa_hee_nalu");
//            valid_qrcodes_map.put("{\"lat\":\"21.331917\",\"long\":\"-158.121817\"}","pele");
//            valid_qrcodes_map.put("{\"lat\":\"21.3317\",\"long\":\"-158.121617\"}","pilikua_nui_wailele");

            //            New gps cords
            valid_qrcodes_map.put("{\"lat\":\"34.031616667\",\"long\":\"-116.943416667\"}","redcoats");
            valid_qrcodes_map.put("{\"lat\":\"34.032483\",\"long\":\"-116.939667\"}","gents");
            valid_qrcodes_map.put("{\"lat\":\"34.0332\",\"long\":\"-116.939266667\"}","bbq_wheel");
            valid_qrcodes_map.put("{\"lat\":\"34.038383333\",\"long\":\"-116.935883333\"}","climbing_rocks_face");
            valid_qrcodes_map.put("{\"lat\":\"34.051466667\",\"long\":\"-116.952533333\"}","squirrel_and_saw");
            valid_qrcodes_map.put("{\"lat\":\"34.0385\",\"long\":\"-116.9371\"}","school_house");
            valid_qrcodes_map.put("{\"lat\":\"34.051133333\",\"long\":\"-116.951566667\"}","feed_station");
            valid_qrcodes_map.put("{\"lat\":\"34.040416667\",\"long\":\"-116.941233333\"}","concervency_sign");
            valid_qrcodes_map.put("{\"lat\":\"34.041016667\",\"long\":\"-116.941366667\"}","wilsher_peak_mountian_siloette");
            valid_qrcodes_map.put("{\"lat\":\"34.038966667\",\"long\":\"-116.9369\"}","tennis_court_net");

            //gps cords without pop-up text
            valid_qrcodes_map.put("{\"lat\":\"34.0406\",\"long\":\"-116.941783333\"}","-1"); //*
            valid_qrcodes_map.put("{\"lat\":\"34.040067\",\"long\":\"-116.941067\"}","-1");
            valid_qrcodes_map.put("{\"lat\":\"34.051367\",\"long\":\"-116.95305\"}","-1");
            valid_qrcodes_map.put("{\"lat\":\"34.0327\",\"long\":\"-116.942967\"}","-1");
            valid_qrcodes_map.put("{\"lat\":\"34.0331\",\"long\":\"-116.939266667\"}","-1");//*
            valid_qrcodes_map.put("{\"lat\":\"34.038383\",\"long\":\"-116.93605\"}","-1");
            valid_qrcodes_map.put("{\"lat\":\"34.038817\",\"long\":\"-116.937183\"}","-1");
            valid_qrcodes_map.put("{\"lat\":\"34.04415\",\"long\":\"-116.947816667\"}","-1");//*
            valid_qrcodes_map.put("{\"lat\":\"34.044416667\",\"long\":\"-116.94775\"}","-1");//*
            valid_qrcodes_map.put("{\"lat\":\"34.045767\",\"long\":\"-116.94535\"}","-1");
            valid_qrcodes_map.put("{\"lat\":\"34.052\",\"long\":\"-116.953889\"}","-1"); //oak glen//*
            valid_qrcodes_map.put("{\"lat\":\"34.099717\",\"long\":\"-117.591167\"}","-1"); //newly added


            for(String str : valid_qrcodes_map.keySet()) {

                    if(!valid_qrcodes_map.get(str).equals("-1"))
                        pop_up_msg = valid_qrcodes_map.get(str);
                    else
                        pop_up_msg = "You have reached your destination!";
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAdviceImagePath != null) {
            publishAdviceImage(mAdviceImagePath);
        } else if (mAdviceImageResId != 0) {
            publishAdviceImage(mAdviceImageResId);
        }
        mMapHandler.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapHandler.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapHandler.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAdviceImageLoaderHandle != null) cancelAdviceImageLoad();
        mMapHandler.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapHandler.onDestroy();

    }

    @Override
    public void onBackPressed() {
        if (mMapHandler.tryExit(this)) {
            super.onBackPressed();
        }
    }

    @Override
    public void onMapLoadingStatusFinished() {
        View loadingView = findViewById(R.id.loadingView);
        assert loadingView != null;
        loadingView.setVisibility(View.GONE);
    }

    @Override
    public void onMapModeChanged(int newMode) {
        switch (newMode) {
            case MapHandler.MODE_FREE:
                BitmapManager.clearImageViewForeground(mAdviceImageView);
                mAdviceTextView.setText("");
                mAdviceHolder.setVisibility(View.GONE);
                mFab.setImageResource(R.drawable.ic_navigation_white_24dp);
                mFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMapHandler.launchNavigation();
                    }
                });
                mWalkButton.setVisibility(View.GONE);
                break;

            case MapHandler.MODE_NAV:
                mAdviceHolder.setVisibility(View.VISIBLE);
                mFab.setImageResource(R.drawable.ic_cancel_white_24dp);
                mFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMapHandler.exitNavigation(false);
                    }
                });

                mWalkButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMapHandler.exitNavigation(false);
                        mMapHandler.startWalkingNavigation();
                    }
                });
                mWalkButton.setVisibility(View.VISIBLE);
                break;

            case MapHandler.MODE_WALKING_NAV:
                mFab.setImageResource(R.drawable.ic_cancel_white_24dp);
                mFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMapHandler.exitWalkingNavigation();
                    }
                });
                mWalkButton.setVisibility(View.GONE);
                break;

            case MapHandler.MODE_POST_NAV_ROAM:
                BitmapManager.clearImageViewForeground(mAdviceImageView);
                mAdviceTextView.setText("");
                mAdviceHolder.setVisibility(View.GONE);
                mFab.setImageResource(0);
                mFab.setOnClickListener(null);
                mWalkButton.setVisibility(View.GONE);
                break;

            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public void publishAdvice(@DrawableRes int adviceImage, @Nullable String adviceText) {
        publishAdviceCommon(publishAdviceImage(adviceImage), publishAdviceText(adviceText));
    }

    @Override
    public void publishAdvice(@Nullable String adviceImage, @Nullable String adviceText) {
        publishAdviceCommon(publishAdviceImage(adviceImage), publishAdviceText(adviceText));
    }

    @Override
    public void publishAdvice(@DrawableRes int adviceImage, int adviceText) {
        publishAdviceCommon(publishAdviceImage(adviceImage), publishAdviceText(adviceText));
    }

    @Override
    public void publishAdvice(@Nullable String adviceImage, int adviceText) {
        publishAdviceCommon(publishAdviceImage(adviceImage), publishAdviceText(adviceText));
    }

    private boolean publishAdviceImage(@DrawableRes int imageResId) {
        cancelAdviceImageLoad();
        mAdviceImageResId = imageResId;
        if (imageResId != 0) {
            mAdviceImageLoaderHandle = BitmapManager.setImageViewBitmap(mAdviceImageView, imageResId,
                    Util.IMAGE_TYPE_THUMBNAIL,
                    (int) getResources().getDimension(R.dimen.advice_image_width),
                    (int) getResources().getDimension(R.dimen.advice_image_height),
                    new BitmapManager.OnImageLoadCompleteListener() {
                        @Override
                        public void onImageLoadComplete() {
                            mAdviceImageLoaderHandle = null;
                            mAdviceImageResId = 0;
                        }
                    });
            return true;
        }
        return false;
    }

    private boolean publishAdviceImage(@Nullable String imagePath) {
        if (imagePath != null && !imagePath.equals("")) {
            mAdviceImageLoaderHandle = BitmapManager.setImageViewBitmap(mAdviceImageView, imagePath,
                    Util.IMAGE_TYPE_THUMBNAIL,
                    (int) getResources().getDimension(R.dimen.advice_image_width),
                    (int) getResources().getDimension(R.dimen.advice_image_height),
                    new BitmapManager.OnImageLoadCompleteListener() {
                        @Override
                        public void onImageLoadComplete() {
                            mAdviceImageLoaderHandle = null;
                            mAdviceImagePath = null;
                        }
                    });
            return true;
        }
        return false;
    }

    private boolean publishAdviceText(@Nullable String adviceText) {
        mAdviceTextView.setText(adviceText);
        return (adviceText != null && !adviceText.equals(""));
    }

    private boolean publishAdviceText(@StringRes int adviceResId) {
        mAdviceTextView.setText(adviceResId);
        return (adviceResId != 0);
    }

    private void publishAdviceCommon(boolean hasImageContent, boolean hasTextContent) {
        mAdviceHolder.setVisibility((hasImageContent && hasTextContent) ? View.VISIBLE : View.GONE);
    }

    private void cancelAdviceImageLoad() {
        if (mAdviceImageLoaderHandle != null) mAdviceImageLoaderHandle.cancel();

        mAdviceImagePath = null;
        mAdviceImageResId = 0;

    }

    @Override
    public void onRouteReady() {
        mFab.setImageResource(R.drawable.ic_navigation_white_24dp);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapHandler.launchNavigation();
            }
        });
        mWaitingIndicator.setVisibility(View.GONE);
    }

    @Override
    public void onRouteCalculationFailed(@NonNull String reason) {
        Snackbar
                .make(mMapView, String.format(Locale.US, "Could not calculate route: %s", reason), Snackbar.LENGTH_LONG)
                .show();
    }

    @Nullable
    @Override
    public TextToSpeech getTts(boolean initializeIfEmpty) {
        if (mTts == null && initializeIfEmpty) mTts = initTts(true);

        return mTts;
    }

    private TextToSpeech initTts(final boolean retryOnError) {
        return new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                boolean success = false;
                if (mTts != null && status == TextToSpeech.SUCCESS) {
                    try {
                        Locale[] locales = {Locale.US, Locale.CANADA, Locale.ENGLISH, Locale.getDefault()};
                        for (Locale locale : locales) {
                            int availability = mTts.isLanguageAvailable(locale);
                            if (availability == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE ||
                                    availability == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                                    availability == TextToSpeech.LANG_AVAILABLE) {
                                int setLanguageResult = mTts.setLanguage(locale);
                                if (setLanguageResult != TextToSpeech.LANG_MISSING_DATA &&
                                        setLanguageResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                                    success = true;
                                    break;
                                }
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        String msg = "Received IllegalArgumentException while initializing Text To Speech. " +
                                "Probably caused by a DeadObjectException. " +
                                (retryOnError ? "Trying again." : "Giving up");
                        Log.w(Util.TAG, msg);
                        mTts = null;
                        if (retryOnError) {
                            initTts(false);
                        } else {
                            success = false;
                        }
                    }
                }
                if (!success) {
                    Snackbar.make(mMapView, "Could not initialize Text to Speech engine", Snackbar.LENGTH_LONG);
                    mTts = null;
                }
            }
        });
    }

    @Override
    public void releaseTts() {
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
            mTts = null;
        }
    }

    @Override
    public boolean onDestinationReached() {
        // Save the fact that this has been visited
        SharedPreferences prefs = getSharedPreferences(Util.SHARED_PREFERENCES_KEY, MODE_PRIVATE);
        prefs.edit().putBoolean(mVisitedPrefKey, true).apply();

        // Notify the user
//        new AlertDialog.Builder(this)
//                .setTitle("Congratulations")
//                .setMessage("You've reached the destination!")
//                .setIcon(mThumbnailId)
//                .setPositiveButton("OK", null)
//                .show();
        String msg = Util.returnDestinationMsg(pop_up_msg, this);
        Util.destinationPopUp(msg, this);

        // Don't continue checking whether the destination has been reached.
        return false;
    }
}
