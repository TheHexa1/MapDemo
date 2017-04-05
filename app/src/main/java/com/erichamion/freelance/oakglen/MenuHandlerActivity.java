package com.erichamion.freelance.oakglen;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.erichamion.freelance.oakglen.bm.BitmapManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eric Ray on 6/24/16.
 *
 */
public abstract class MenuHandlerActivity extends AppCompatActivity {

    //public static final String PREFKEY_SIMULATENAV = "simulateNav";
    //public static final String PREFKEY_NAVUSEAUDIO = "navUseAudio";
    private static final String INTENTKEY_BACKGROUND_GRAVITY = "backgroundGravity";
    private static final String INTENTKEY_BACKGROUND_ID = "backgroundId";
    private MenuItem mAutoConnectMenuItem;
    private boolean mIsEnabled;
    private boolean mShouldBeEnabled;

    private BitmapManager.ImageLoaderHandle mBackgroundLoaderHandle;
    private boolean mWasBackgroundImageCanceled = false;
    private WeakReference<View> mMainBackgroundViewRef;
    private int mBackgroundImageResourceId;
    private int mBackgroundImageGravity;

    private final BitmapManager.OnImageLoadCompleteListener mBackgroundImageListener =
            new BitmapManager.OnImageLoadCompleteListener() {
                @Override
                public void onImageLoadComplete() {
                    mBackgroundLoaderHandle = null;
                    mWasBackgroundImageCanceled = false;
                }
            };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.initBitmapManagerIfNeeded();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBackgroundLoaderHandle != null) {
            mBackgroundLoaderHandle.cancel();
            mWasBackgroundImageCanceled = true;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mWasBackgroundImageCanceled) {
            setMainBackground(mMainBackgroundViewRef.get(), mBackgroundImageResourceId, mBackgroundImageGravity);
        }
    }

    final void setMainBackground(@Nullable View view, @DrawableRes int imageId, int gravity) {
        mWasBackgroundImageCanceled = false;
        if (mBackgroundLoaderHandle != null) {
            mBackgroundLoaderHandle.cancel();
        }
        mBackgroundLoaderHandle = null;

        if (view == null || imageId == 0) {
            if (mMainBackgroundViewRef != null && mMainBackgroundViewRef.get() != null) {
                BitmapManager.clearViewBackground(mMainBackgroundViewRef.get());
            }
            mMainBackgroundViewRef = null;
            return;
        }

        mMainBackgroundViewRef = new WeakReference<>(view);
        mBackgroundImageResourceId = imageId;
        mBackgroundImageGravity = gravity;

        if (view.getWidth() != 0 && view.getHeight() != 0) {
            setMainBackgroundImmediately(view, imageId, gravity);
        } else {
            Util.setSingleUseLayoutChangeListener(view, new Util.LayoutChangeListener() {
                @Override
                public void onLayoutChange(View v) {
                    setMainBackgroundImmediately(v, mBackgroundImageResourceId, mBackgroundImageGravity);
                }
            });
        }
    }

    final void setMainBackground(@Nullable View view, @Nullable Bundle extras) {
        if (view == null || extras == null) {
            setMainBackground(null, 0, 0);
            return;
        }

        @DrawableRes int resId = extras.getInt(INTENTKEY_BACKGROUND_ID, 0);
        int gravity = extras.getInt(INTENTKEY_BACKGROUND_GRAVITY, 0);
        setMainBackground(view, resId, gravity);
    }

    private void setMainBackgroundImmediately(@NonNull View view, @DrawableRes int imageId, int gravity) {
        mBackgroundLoaderHandle = BitmapManager.setViewBackground(view, imageId, gravity, Util.IMAGE_TYPE_TRUE_FULLSCREEN,
                view.getWidth(), view.getHeight(), mBackgroundImageListener);
    }


    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.action_menu, menu);

        mAutoConnectMenuItem = menu.findItem(R.id.menuAutoConnect);
//        mNavUseAudioMenuItem = menu.findItem(R.id.menuNavUseAudio);
//        mSimulateNavigationMenuItem = menu.findItem(R.id.menuSimulateNavigation);

//        SharedPreferences prefs = getSharedPreferences(Util.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
//        mSimulateNavigationMenuItem.setChecked(prefs.getBoolean(Util.PREFKEY_SIMULATENAV, false));
//        mNavUseAudioMenuItem.setChecked(prefs.getBoolean(Util.PREFKEY_NAVUSEAUDIO, false));

        onCreateOptionsMenuEx(menu);
        return true;
    }

    @SuppressWarnings({"WeakerAccess", "EmptyMethod"})
    protected void onCreateOptionsMenuEx(@SuppressWarnings("UnusedParameters") Menu menu) {}

    @Override
    public final boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mIsEnabled != mShouldBeEnabled) {
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setEnabled(mShouldBeEnabled);
            }
            mIsEnabled = mShouldBeEnabled;
        }

        // This should be checked every time the menu is prepared, in case something
        // happened to the service.
        mAutoConnectMenuItem.setChecked(Util.isWiFiServiceRunning(this));

        onPrepareOptionsMenuEx(menu);
        return true;
    }

    @SuppressWarnings({"WeakerAccess", "EmptyMethod"})
    protected void onPrepareOptionsMenuEx(@SuppressWarnings("UnusedParameters") Menu menu) {}

    @Override
    public final boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuFeedback:
                menuStartFeedbackActivity();
                break;

            case R.id.menuAboutApp:
                menuStartAboutActivity(AboutActivity.ABOUT_APP);
                break;

            case R.id.menuAboutTown:
                menuStartAboutActivity(AboutActivity.ABOUT_TOWN);
                break;

            case R.id.menuAutoConnect:
                menuSetAutoConnect(!item.isChecked());
                break;

//            case R.id.menuSimulateNavigation:
//                menuSetSimulateNavigation(!item.isChecked());
//                break;
//
//            case R.id.menuNavUseAudio:
//                menuSetNavUseAudio(!item.isChecked());
//                break;

            default:
                return onOptionsItemSelectedEx(item) || super.onOptionsItemSelected(item);
        }

        return true;
    }

    @SuppressWarnings({"WeakerAccess", "SameReturnValue"})
    protected boolean onOptionsItemSelectedEx(@SuppressWarnings("UnusedParameters") MenuItem item) {
        return false;
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        List<String> denied = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                denied.add(permissions[i]);
            }
        }


        if (Util.onRequestPermissionsResult(this, requestCode, denied)) return;
        if (onRequestPermissionsResultEx(requestCode, denied)) return;

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    boolean onRequestPermissionsResultEx(@SuppressWarnings("unused") int requestCode,
                                         @NonNull @SuppressWarnings("unused") List<String> deniedPermissions) {
        return false;
    }

    final void enableMenu(boolean enabled) {
        mShouldBeEnabled = enabled;
        updateMenu();
    }

    private void updateMenu() {
        supportInvalidateOptionsMenu();
    }

    private void menuStartFeedbackActivity() {
        Intent intent = new Intent(this, FeedbackActivity.class);
        addBackgroundExtras(intent);
        startActivity(intent);
    }

    public void menuStartAboutActivity(int aboutType) {
        Intent intent = new Intent(this, AboutActivity.class);
        intent.putExtra(AboutActivity.ABOUT_TYPE_KEY, aboutType);
        addBackgroundExtras(intent);
        startActivity(intent);
    }

    private void menuSetAutoConnect(boolean autoConnect) {
        if (autoConnect) {
            Util.startWiFiService(this);
        } else {
            Util.stopWiFiService(this);
        }
        mAutoConnectMenuItem.setChecked(autoConnect);
    }

    public final void addBackgroundExtras(Intent intent) {
        intent.putExtra(INTENTKEY_BACKGROUND_ID, mBackgroundImageResourceId);
        intent.putExtra(INTENTKEY_BACKGROUND_GRAVITY, mBackgroundImageGravity);
    }

//    protected final void menuSetSimulateNavigation(boolean simulate) {
//        SharedPreferences prefs = getSharedPreferences(Util.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
//        prefs.edit()
//                .putBoolean(Util.PREFKEY_SIMULATENAV, simulate)
//                .apply();
//        if (mSimulateNavigationMenuItem != null) mSimulateNavigationMenuItem.setChecked(simulate);
//    }

//    protected final void menuSetNavUseAudio(boolean useAudio) {
//        SharedPreferences prefs = getSharedPreferences(Util.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
//        prefs.edit()
//                .putBoolean(Util.PREFKEY_NAVUSEAUDIO, useAudio)
//                .apply();
//        if (mNavUseAudioMenuItem != null) mNavUseAudioMenuItem.setChecked(useAudio);
//    }
}
