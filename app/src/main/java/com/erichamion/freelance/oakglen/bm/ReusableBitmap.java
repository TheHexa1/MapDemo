package com.erichamion.freelance.oakglen.bm;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.erichamion.freelance.oakglen.R;
import com.erichamion.freelance.oakglen.Util;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Holds a Bitmap and associated information (including the Bitmap's source location,
 * the size class, and usage information) facilitating the Bitmap's reuse.
 *
 * Created by Eric Ray on 7/14/2016.
 */
class ReusableBitmap extends ReusableBitmapAbstract {

    public final Bitmap bitmap;
    private final Set<WeakReference<? extends View>> mAttachedViews = new HashSet<>();
    public final BitmapSource src;
    public final int displayType;
    public final int byteSize;

    @Nullable
    private static ViewTag getRBitmapTag(@Nullable View v) throws ClassCastException {
        if (v == null) return null;

        Object rawTag = v.getTag(R.id.reusableBitmapAttachTagKey);
        if (rawTag == null) return null;
        @SuppressWarnings("unchecked")
        ViewTag result = (ViewTag) rawTag;

        return result;
    }

    @Nullable
    public static ReusableBitmap getForegroundRBitmap(@Nullable ImageView imageView) throws ClassCastException {
        ViewTag tag = getRBitmapTag(imageView);
        return (tag == null) ? null : tag.getForeground();
    }

    @Nullable
    public static ReusableBitmap getBackgroundRBitmap(@Nullable View v) throws ClassCastException {
        ViewTag tag = getRBitmapTag(v);
        return (tag == null) ? null : tag.getBackground();
    }

    @Nullable
    public static ReusableBitmap clearImageViewForeground(ImageView v) {
        return clearView(v, true, false)[0];
    }

    @Nullable
    public static ReusableBitmap clearViewBackground(View v) {
        return clearView(v, false, true)[1];
    }

    // Throws ClassCastException if clearForeground is true but v is not an ImageView.
    @NonNull
    private static ReusableBitmap[] clearView(View v, boolean clearForeground, boolean clearBackground) {
        ReusableBitmap[] result = new ReusableBitmap[2];

        ViewTag tag = getRBitmapTag(v);
        ImageView imageView = clearForeground ? (ImageView) v : null;

        if (tag == null) {
            if (clearForeground) {
                imageView.setImageBitmap(null);
                imageView.setImageResource(0);
            }
            if (clearBackground) {
                Util.setViewBackgroundCompat(v, null);
            }
            return result;
        }

        if (clearForeground) {
            ReusableBitmap foreground = tag.getForeground();
            if (foreground != null) foreground.detachFromImageView(imageView);
            result[0] = foreground;
        }

        if (clearBackground) {
            ReusableBitmap background = tag.getBackground();
            if (background != null) background.detachFromViewBackground(v);
            result[1] = background;
        }

        return result;
    }

    ReusableBitmap(Bitmap bitmap, @NonNull BitmapSource src, int displayType) {


        this.bitmap = bitmap;
        this.src = src;
        this.displayType = displayType;
        byteSize = getInitialByteSize();
    }

    @Override
    public int getByteSize() {
        return byteSize;
    }

    @Override
    public int getDisplayType() {
        return displayType;
    }

    @NonNull
    @Override
    public BitmapSource getSrc() {
        return src;
    }

    private int getInitialByteSize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bitmap.getAllocationByteCount();
        } else {
            return bitmap.getWidth() * bitmap.getHeight() * BitmapManager.getBytesPerPixel(bitmap.getConfig());
        }
    }

    public boolean isAttached() {
        return (!mAttachedViews.isEmpty());
    }

    public boolean isAttachedWithAudit() {
        Iterator<WeakReference<? extends View>> iter = mAttachedViews.iterator();
        while (iter.hasNext()) {
            View v = iter.next().get();
            ViewTag tag = (v == null) ? null : getRBitmapTag(v);
            if (tag == null || (tag.getForeground() != this && tag.getBackground() != this)) {
                // View has been garbage collected or is not attached to this ReusableBitmap
                iter.remove();
            }
        }
        return !mAttachedViews.isEmpty();
    }

    public void attachToImageView(ImageView v) {
        // This shouldn't be necessary if everything is being called correctly,
        // but be safe.
        clearImageViewForeground(v);

        v.setImageBitmap(bitmap);

        updateViewTag(v, this, false);
        mAttachedViews.add(new WeakReference<>(v));
    }

    public void attachToViewBackground(View v, int gravity) {
        // This shouldn't be necessary if everything is being called correctly,
        // but be safe.
        clearViewBackground(v);
        BitmapDrawable drawable = new BitmapDrawable(v.getResources(), bitmap);
        drawable.setGravity(gravity);
        Util.setViewBackgroundCompat(v, drawable);

        updateViewTag(v, this, false);
        mAttachedViews.add(new WeakReference<>(v));
    }

    private static void updateViewTag(@NonNull View v, @Nullable ReusableBitmap rBitmap, boolean setBackground) {
        ViewTag result = getRBitmapTag(v);
        if (result == null) {
            result = new ViewTag();
        }
        if (setBackground) {
            result.setBackground(rBitmap);
        } else {
            result.setForeground(rBitmap);
        }
        if (result.getForeground() == null && result.getBackground() == null) {
            result = null;
        }
        v.setTag(R.id.reusableBitmapAttachTagKey, result);
    }

    private void detachFromImageView(@NonNull ImageView v) {
        // Remove the target view from the list of attached views, doing some partial
        // cleanup along the way.
        Iterator<WeakReference<? extends View>> iter = mAttachedViews.iterator();
        ViewTag tag = null;
        while (iter.hasNext()) {
            View currentView = iter.next().get();
            tag = getRBitmapTag(currentView);
            if (tag == null || (tag.getForeground() != this && tag.getBackground() != this) || currentView == v) {
                // Either this is the target view, it has been garbage collected, or
                // it doesn't point to this ReusableBitmap. Remove it from the set of
                // attached views.
                iter.remove();

                if (currentView == v) {
                    // If this is the target view, stop looking. We're not trying to do
                    // a full audit, just opportunistically cleaning up anything we find
                    // on the way to the target.
                    break;
                }
            }
        }

        // Make sure the target view no longer points to this ReusableBitmap,
        if (tag != null && tag.getForeground() == this) {
            updateViewTag(v, null, false);
        }
    }

    private void detachFromViewBackground(@NonNull View v) {
        // Remove the target view from the list of attached views, doing some partial
        // cleanup along the way.
        Iterator<WeakReference<? extends View>> iter = mAttachedViews.iterator();
        ViewTag tag = null;
        while (iter.hasNext()) {
            View currentView = iter.next().get();
            tag = getRBitmapTag(currentView);
            if (tag == null || (tag.getForeground() != this && tag.getBackground() != this) || currentView == v) {
                // Either this is the target view, it has been garbage collected, or
                // it doesn't point to this ReusableBitmap. Remove it from the set of
                // attached views.
                iter.remove();

                if (currentView == v) {
                    // If this is the target view, stop looking. We're not trying to do
                    // a full audit, just opportunistically cleaning up anything we find
                    // on the way to the target.
                    break;
                }
            }
        }

        // Make sure the target view no longer points to this ReusableBitmap.
        if (tag != null && tag.getBackground() == this) {
            updateViewTag(v, null, true);
        }
    }


    private static class ViewTag {
        private WeakReference<ReusableBitmap> foregroundRef;
        private WeakReference<ReusableBitmap> backgroundRef;

        @Nullable
        public ReusableBitmap getForeground() {
            return (foregroundRef == null) ? null : foregroundRef.get();
        }

        @Nullable
        public ReusableBitmap getBackground() {
            return (backgroundRef == null) ? null : backgroundRef.get();
        }

        public void setForeground(@Nullable ReusableBitmap rBitmap) {
            foregroundRef = (rBitmap == null) ? null : new WeakReference<>(rBitmap);
        }

        public void setBackground(@Nullable ReusableBitmap rBitmap) {
            backgroundRef = (rBitmap == null) ? null : new WeakReference<>(rBitmap);
        }
    }

}
