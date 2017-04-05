package com.erichamion.freelance.oakglen.bm;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.erichamion.freelance.oakglen.R;
import com.erichamion.freelance.oakglen.Util;

import java.lang.ref.WeakReference;

/**
 * Uses a background thread to load a memory-optimized bitmap from resources
 * or from a filesystem path. Uses an inBitmapSupplier to attempt reusing
 * existing bitmap memory. Notifies an OnImageLoadTaskCompleteListener upon
 * completion.
 *
 * Created by erich on 7/14/2016.
 */
class ImageLoaderTask extends AsyncTask<ImageLoaderTask.ImageLoaderWorkItem, Void, ReusableBitmapAbstract> {
    private final OnImageLoadTaskCompleteListener mListener;
    private final InBitmapSupplier mInBitmapSupplier;

    @SuppressWarnings("SameParameterValue")
    public ImageLoaderTask(@NonNull OnImageLoadTaskCompleteListener listener, @NonNull InBitmapSupplier inBitmapSupplier) {
        super();
        mListener = listener;
        mInBitmapSupplier = inBitmapSupplier;
    }

    @Override
    @Nullable
    protected ReusableBitmapAbstract doInBackground(ImageLoaderWorkItem... params) {
        if (params.length != 1) throw new AssertionError();

        ImageLoaderWorkItem workItem = params[0];

        if (isCancelled()) return null;
        if (workItem.getDest() == null)
            return new EmptyReusableBitmap(workItem.src, workItem.displayType);

        Bitmap result = loadSampledBitmap(workItem);
        if (result == null) return new EmptyReusableBitmap(workItem.src, workItem.displayType);

        return new ReusableBitmap(result, workItem.src, workItem.displayType);
    }

    @Override
    protected void onPostExecute(@NonNull ReusableBitmapAbstract result) {
        if (result instanceof ReusableBitmap) {
            mListener.onImageLoadComplete((ReusableBitmap) result);
        } else {
            if (!(result instanceof EmptyReusableBitmap)) throw new AssertionError();
            mListener.onImageLoadFailed((EmptyReusableBitmap) result);
        }
    }

    @Override
    protected void onCancelled(ReusableBitmapAbstract result) {
        // If a result was generated before the task could be cancelled,
        // go ahead and report the result. It may be cached for later
        // reuse.
        if (result != null) onPostExecute(result);
    }

    @Nullable
    private Bitmap loadSampledBitmap(@NonNull ImageLoaderWorkItem workItem) {
        BitmapFactory.Options options = createBitmapOptionsForSize(workItem);

        reduceBitmapOptionsMemory(workItem.maxMem, options);

        return loadBitmapFromOptions(workItem, options);
    }

    @Nullable
    private Bitmap loadBitmapFromOptions(@NonNull ImageLoaderWorkItem workItem, @NonNull BitmapFactory.Options options) {
        Bitmap retVal = null;

        options.inBitmap = mInBitmapSupplier.getInBitmap(options);
        if (options.inBitmap != null) {
            if (!options.inBitmap.isMutable())
                throw new AssertionError("Attempted to reuse immutable bitmap");


            Log.d(Util.TAG, "Attempting to reuse existing bitmap memory");
        }
        int triesLeft = (options.inBitmap == null) ? 1 : 2;
        while (retVal == null && triesLeft > 0) {
            try {
                if (workItem.src.sourceType == R.id.bitmapSourceResId) {
                    retVal = BitmapFactory.decodeResource(workItem.resources, workItem.src.resId, options);
                } else {
                    retVal = BitmapFactory.decodeFile(workItem.src.path, options);
                }
            } catch (IllegalArgumentException e) {
                // Do nothing. If BitmapFactory.decode* fails, it will either
                // return null or throw this exception (the docs state it will
                // return null AND throw IllegalArgumentException, but that's
                // not possible). In either case, retVal remains null, and the
                // situation is handled below.
            }

            // If we need to try again, do it without the inBitmap.
            options.inBitmap = null;
            triesLeft--;
        }

        return retVal;
    }

    private void reduceBitmapOptionsMemory(long maxMem, @NonNull BitmapFactory.Options options) {
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        while (BitmapManager.getBitmapMemorySizeFromOptions(options) > maxMem) {
            options.inSampleSize *= 2;
        }
    }

    private BitmapFactory.Options createBitmapOptionsForSize(@NonNull ImageLoaderWorkItem workItem) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inMutable = true;
        options.inScaled = false;
        if (workItem.src.sourceType == R.id.bitmapSourceResId) {
            BitmapFactory.decodeResource(workItem.resources, workItem.src.resId, options);
        } else {
            BitmapFactory.decodeFile(workItem.src.path, options);
        }

        int doubleSampleSize = 2;
        while (options.outWidth / doubleSampleSize >= workItem.width && options.outHeight / doubleSampleSize >= workItem.height) {
            doubleSampleSize *= 2;
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = doubleSampleSize / 2;

        return options;
    }

    public interface InBitmapSupplier {
        @Nullable
        Bitmap getInBitmap(BitmapFactory.Options options);
    }

    public interface OnImageLoadTaskCompleteListener {
        void onImageLoadComplete(ReusableBitmap rBitmap);

        void onImageLoadFailed(EmptyReusableBitmap rBitmap);

        void onImageLoadCancelled(BitmapManager.ImageLoaderHandle handle);
    }

    public static class ImageLoaderWorkItem {
        private final WeakReference<View> destRef;
        public final BitmapSource src;
        public final int displayType;
        public final int width;
        public final int height;
        public final long maxMem;
        public final Resources resources;

        public ImageLoaderWorkItem(@NonNull View dest, @NonNull BitmapSource src, @IdRes int displayType, int width, int height, long maxMem) {
            this.destRef = new WeakReference<>(dest);
            this.src = src;
            this.displayType = displayType;
            this.width = width;
            this.height = height;
            this.resources = dest.getContext().getApplicationContext().getResources();
            this.maxMem = maxMem;
        }

        public
        @Nullable
        View getDest() {
            return destRef.get();
        }
    }
}
