package com.erichamion.freelance.oakglen;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.graphics.drawable.DrawableWrapper;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RatingBar;

import com.erichamion.freelance.oakglen.bm.BitmapManager;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.SKMapsInitSettings;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.skobbler.ngx.navigation.SKAdvisorSettings;
import com.skobbler.ngx.util.SKLogging;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Eric Ray on 6/2/16.
 *
 */
public class Util {
    public static final String SHARED_PREFERENCES_KEY = "OakGlenPreferences";
    public static final String PREFKEY_EULA = "eulaAccepted";
    public static final String PREFKEY_AUTOCONNECT = "wifiAutoConnectEnabled";
    public static final String PREFKEY_MAPRESOURCESPATH = "mapResourcesPath";
    public static final String TAG = "Discover Oak Glen";
    /**
     * Small thumbnail-sized image
     */
    public static final int IMAGE_TYPE_THUMBNAIL = R.id.bitmapTypeThumbnail;
    /**
     * Large image, typically the full width of the screen or nearly so
     */
    public static final int IMAGE_TYPE_FULL_WIDTH = R.id.bitmapTypeFullWidth;
    /**
     * Fullscreen (or nearly so) image, in both dimensions, not just width
     */
    public static final int IMAGE_TYPE_TRUE_FULLSCREEN = R.id.bitmapTypeTrueFullscreen;

    private static final int MAX_THUMBNAILS = 40;
    private static final int MAX_FULL_WIDTH = 12;
    private static final int MAX_TRUE_FULLSCREEN = 5;

    private static final int PERM_REQUEST = R.id.permissionRequestMenu & ((1 << 16) - 1);

    public static void initBitmapManagerIfNeeded() {
        if (BitmapManager.getImageTypeCount() >= 3) return;

        BitmapManager.addImageType(IMAGE_TYPE_THUMBNAIL, MAX_THUMBNAILS);
        BitmapManager.addImageType(IMAGE_TYPE_FULL_WIDTH, MAX_FULL_WIDTH);
        BitmapManager.addImageType(IMAGE_TYPE_TRUE_FULLSCREEN, MAX_TRUE_FULLSCREEN);
    }

    public static Pair<String, String> getCoordinateStrings(double latitude, double longitude) {
        String latText = latitude >= 0 ? "N" : "S";
        latText += Double.toString(Math.abs(latitude));
        String longText = longitude >= 0 ? "E" : "W";
        longText += Double.toString(Math.abs(longitude));

        return new Pair<>(latText, longText);
    }

    public static void setViewBackgroundCompat(@NonNull View view, @Nullable Drawable background) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(background);
        } else {
            //noinspection deprecation
            view.setBackgroundDrawable(background);
        }
    }

    public static void initMapLibrary(Context context, String mapResourcesPath) {
        SKLogging.enableLogs(true);

        SKMapsInitSettings initSettings = new SKMapsInitSettings();

        initSettings.setMapResourcesPaths(mapResourcesPath, new SKMapViewStyle(mapResourcesPath + "daystyle/", "daystyle.json"));

        SKAdvisorSettings advisorSettings = initSettings.getAdvisorSettings();
        advisorSettings.setAdvisorConfigPath(mapResourcesPath + "/Advisor");
        advisorSettings.setResourcePath(mapResourcesPath + "/Advisor/Languages");
        advisorSettings.setLanguage(SKAdvisorSettings.SKAdvisorLanguage.LANGUAGE_EN);
        advisorSettings.setAdvisorVoice("en");
        initSettings.setAdvisorSettings(advisorSettings);

        // EXAMPLE OF ADDING PREINSTALLED MAPS
//         initMapSettings.setPreinstalledMapsPath(((DemoApplication)context.getApplicationContext()).getMapResourcesDirPath()
//         + "/PreinstalledMaps");
        // initMapSettings.setConnectivityMode(SKMaps.CONNECTIVITY_MODE_OFFLINE);

        SKMaps.getInstance().initializeSKMaps(context, initSettings);
    }

    public static void setSingleUseLayoutChangeListener(View view, LayoutChangeListener listener) {
        final WeakReference<LayoutChangeListener> listenerRef = new WeakReference<>(listener);
        final WeakReference<View> viewRef = new WeakReference<>(view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                private boolean hasBeenCalled = false;

                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    // This shouldn't be needed because the listener is being unregistered,
                    // but on some devices it still continues to be called.
                    if (hasBeenCalled) return;
                    hasBeenCalled = true;

                    v.removeOnLayoutChangeListener(this);
                    LayoutChangeListener layoutChangeListener = listenerRef.get();
                    if (layoutChangeListener != null) {
                        layoutChangeListener.onLayoutChange(v);
                    }
                }
            });
        } else {
            final ViewTreeObserver observer = view.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                private boolean hasBeenCalled = false;

                @Override
                public void onGlobalLayout() {
                    // This shouldn't be needed because the listener is being unregistered,
                    // but on some devices it still continues to be called.
                    if (hasBeenCalled) return;
                    hasBeenCalled = true;


                    if (observer.isAlive()) {
                        //noinspection deprecation
                        observer.removeGlobalOnLayoutListener(this);
                    }
                    View theView = viewRef.get();
                    if (theView != null) {
                        LayoutChangeListener layoutChangeListener = listenerRef.get();
                        if (layoutChangeListener != null) {
                            layoutChangeListener.onLayoutChange(theView);
                        }
                    }
                }
            });
        }
    }

    public static String getVisitedPrefkey(int chapterIndex, int pageIndex) {
        return String.format(Locale.US, "visitedCh%dP%d", chapterIndex, pageIndex);
    }

    public interface LayoutChangeListener {
        void onLayoutChange(View v);
    }

//    public static void setRatingBarToAccentColor(@NonNull RatingBar ratingBar, @Nullable Resources.Theme theme) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) return;
//
//        int accentColor = ResourcesCompat.getColor(ratingBar.getResources(), R.color.colorAccent, theme);
//
//        Drawable drawable = ratingBar.getProgressDrawable();
//        LayerDrawable layerDrawable;
//        if (drawable instanceof LayerDrawable) {
//            layerDrawable = (LayerDrawable) drawable;
//        } else if (drawable instanceof DrawableWrapper) {
//            // I don't believe this cast should fail
//            layerDrawable = (LayerDrawable) ((DrawableWrapper) drawable).getWrappedDrawable();
//        } else {
//            // This shouldn't happen. If it does, we'll throw a NullPointerException
//            // a few lines down, so we'll know it needs fixed.
//            layerDrawable = null;
//        }
//
//        // layerDrawable.getDrawable(0) is empty star background, leave it alone.
//        // layerDrawable.getDrawable(1) is partial star background
//        // layerDrawable.getDrawable(2) is filled star background
//        assert layerDrawable != null;
//        DrawableCompat.setTint(DrawableCompat.wrap(layerDrawable.getDrawable(1)), accentColor);
//        DrawableCompat.setTint(DrawableCompat.wrap(layerDrawable.getDrawable(2)), accentColor);
//    }

    public static void setRatingBarToColoredImage(@NonNull ImageView visitedIndicator) {

        visitedIndicator.setImageResource(R.drawable.colored_apple);
    }



    /**
     * Modified from the CIM Auto-Connect app,
     * package com.nerrdit.freelancer.autoconnector
     */
    public static boolean isWiFiServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (WiFiService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void startWiFiService(AppCompatActivity activityContext) {
        List<String> missingPerms = getMissingPermissions(activityContext,
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE);

        if (!missingPerms.isEmpty()) {
            ActivityCompat.requestPermissions(activityContext, missingPerms.toArray(new String[missingPerms.size()]), PERM_REQUEST);
        } else {
            startWiFiServiceInternal(activityContext);
        }


    }

    private static void startWiFiServiceInternal(Context context) {
        if (!isWiFiServiceRunning(context)) {
            Intent intent = new Intent(context.getApplicationContext(), WiFiService.class);
            context.startService(intent);
        }
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREFKEY_AUTOCONNECT, true);
        editor.apply();
    }

    public static void stopWiFiService(Context context) {
        context.stopService(new Intent(context.getApplicationContext(), WiFiService.class));
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREFKEY_AUTOCONNECT, false);
        editor.apply();
    }

public static List<String> getMissingPermissions(Context context, String... perms) {
        List<String> result = new ArrayList<>();
        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                result.add(perm);
            }
        }

        return result;
    }

    public static boolean onRequestPermissionsResult(Context context, int requestCode, @NonNull List<String> deniedPermissions) {
        if (requestCode == PERM_REQUEST) {
            if (deniedPermissions.isEmpty()) {
                startWiFiServiceInternal(context.getApplicationContext());
            } else {
                new AlertDialog.Builder(context)
                        .setTitle("About Permissions")
                        .setMessage(R.string.permission_explanation)
                        .setIcon(0)
                        .setNeutralButton("Got It", null)
                        .show();
            }
            return true;
        } else {
            return false;
        }
    }

    public static void destinationPopUp(String msg, Context context){

        android.support.v7.app.AlertDialog.Builder alertDialog = new android.support.v7.app.AlertDialog.Builder(context);

        // Setting Dialog Message
        alertDialog.setMessage(msg);

        // on pressing OK button
        alertDialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        android.support.v7.app.AlertDialog alert = alertDialog.create();
        alert.show(); //show alert message
    }

    public static String returnDestinationMsg(String destination_name, Context context){
        switch(destination_name){
            case "aloha":
                return context.getString(R.string.aloha);
            case "ana_puka":
                return context.getString(R.string.ana_puka);
            case "hao_hakahaka":
                return context.getString(R.string.hao_hakahaka);
            case "honu":
                return context.getString(R.string.honu);
            case "hooikaika_kino":
                return context.getString(R.string.hooikaika_kino);
            case "i_a":
                return context.getString(R.string.i_a);
            case "la_au":
                return context.getString(R.string.la_au);
            case "lahalaha_wai":
                return context.getString(R.string.lahalaha_wai);
            case "lomi":
                return context.getString(R.string.lomi);
            case "mea_pani":
                return context.getString(R.string.mea_pani);
            case "pahu":
                return context.getString(R.string.pahu);
            case "papa_hee_nalu":
                return context.getString(R.string.papa_hee_nalu);
            case "pele":
                return context.getString(R.string.pele);
            case "pilikua_nui_wailele":
                return context.getString(R.string.pilikua_nui_wailele);
            case "redcoats":
                return context.getString(R.string.redcoats);
            case "gents":
                return context.getString(R.string.gents);
            case "bbq_wheel":
                return context.getString(R.string.bbq_wheel);
            case "climbing_rocks_face":
                return context.getString(R.string.climbing_rocks_face);
            case "squirrel_and_saw":
                return context.getString(R.string.squirrel_and_saw);
            case "school_house":
                return context.getString(R.string.school_house);
            case "feed_station":
                return context.getString(R.string.feed_station);
            case "concervency_sign":
                return context.getString(R.string.concervency_sign);
            case "wilsher_peak_mountian_siloette":
                return context.getString(R.string.wilsher_peak_mountian_siloette);
            case "tennis_court_net":
                return context.getString(R.string.tennis_court_net);
            default:
                return "You have reached your destination!";
        }
    }


}
