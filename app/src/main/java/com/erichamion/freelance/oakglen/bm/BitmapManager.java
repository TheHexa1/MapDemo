package com.erichamion.freelance.oakglen.bm;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.erichamion.freelance.oakglen.Util;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Creates Bitmaps and assigns them to ImageViews and to View backgrounds. Uses
 * an internal cache with a fixed maximum size to reuse bitmaps as appropriate,
 * as well as to reclaim memory from previously used bitmaps. Bitmaps
 * with the same size class (as defined using
 * {@link BitmapManager#addImageType(int, int)} and same resource location are
 * considered identical regardless of their actual heights and widths.
 * Use {@link BitmapManager#setImageViewBitmap(ImageView, int, int, int, int, OnImageLoadCompleteListener)},
 * {@link BitmapManager#setImageViewBitmap(ImageView, String, int, int, int, OnImageLoadCompleteListener)},
 * or the equivalent setViewBackground() methods
 * to simultaneously load a Bitmap from resources and assign it to an
 * ImageView or a View background. When the Bitmap is no longer needed, call
 * {@link BitmapManager#clearImageViewForeground(ImageView)} or
 * {@link BitmapManager#clearViewBackground(View)} to unlink from the Bitmap.
 * Calling code does not need to (and generally should not) manipulate the
 * Bitmap object directly.
 * Created by Eric Ray on 6/19/16.
 */
public class BitmapManager {

    private static int nextRequestNumber = 1;

    private static final double CACHE_MEM_FRACTION = 0.5;

    private static final BitmapCache mCache = new BitmapCache((long) (Runtime.getRuntime().maxMemory() * CACHE_MEM_FRACTION));

    private static final Map<Integer, Integer> mImageTypeLimits = new HashMap<>();
    private static final Map<ReusableBitmapAbstract, Pair<ImageLoaderTask, List<ImageLoadCompletionItem>>> mPendingTasks = new HashMap<>();

    private static final ImageLoaderTask.OnImageLoadTaskCompleteListener mOnImageLoadCompleteListener = new ImageLoaderTask.OnImageLoadTaskCompleteListener() {
        @Override
        public void onImageLoadComplete(@NonNull ReusableBitmap rBitmap) {
            Pair<ImageLoaderTask, List<ImageLoadCompletionItem>> pair = mPendingTasks.get(rBitmap);
            if (pair == null) {
                // All tasks for this image have been cancelled. Save the bitmap for possible
                // later reuse if there is room.
                if (mCache.getFreeSpace() >= rBitmap.byteSize) {
                    mCache.addBitmap(rBitmap);
                }
                return;
            }


            List<ImageLoadCompletionItem> loadCompletionItems = pair.second;
            for (ImageLoadCompletionItem completionItem : loadCompletionItems) {
                if (completionItem.getImageView() != null) {
                    rBitmap.attachToImageView(completionItem.getImageView());
                } else if (completionItem.getBackgroundView() != null) {
                    rBitmap.attachToViewBackground(completionItem.getBackgroundView(), completionItem.gravity);
                }
                if (completionItem.getListener() != null) {
                    completionItem.getListener().onImageLoadComplete();
                }
            }
            mCache.addBitmap(rBitmap);

            pair.second.clear();
            mPendingTasks.remove(rBitmap);
        }

        @Override
        public void onImageLoadFailed(@NonNull EmptyReusableBitmap rBitmap) {
            Pair<ImageLoaderTask, List<ImageLoadCompletionItem>> pair = mPendingTasks.get(rBitmap);
            if (pair != null) {
                pair.second.clear();
            }
            mPendingTasks.remove(rBitmap);
        }

        @Override
        public void onImageLoadCancelled(ImageLoaderHandle handle) {
            ImageLoaderTask task = handle.getTask();
            if (task == null) {
                // Task is already gone, nothing to do.
                // mPendingTasks keeps hard references to tasks, so it
                // must be already removed from the map.
                return;
            }

            Pair<ImageLoaderTask, List<ImageLoadCompletionItem>> pair = mPendingTasks.get(handle.comparisonRBitmap);
            if (pair == null) return;

            List<ImageLoadCompletionItem> completionItems = pair.second;
            Iterator<ImageLoadCompletionItem> iter = completionItems.iterator();
            while (iter.hasNext()) {
                ImageLoadCompletionItem completionItem = iter.next();
                if (completionItem.requestNumber == handle.requestNumber) {
                    iter.remove();
                    break;
                }
            }
            if (completionItems.isEmpty()) {
                // This was the only item waiting on this task.
                mPendingTasks.remove(handle.comparisonRBitmap);
                task.cancel(true);
            }

        }
    };

    private BitmapManager() {
        throw new UnsupportedOperationException("BitmapManager should not be instantiated");
    }

    public static void addImageType(int imageType, int maxOfType) {
        mImageTypeLimits.put(imageType, maxOfType);
    }

    public static int getImageTypeCount() {
        return mImageTypeLimits.size();
    }

    @Nullable
    public static ImageLoaderHandle setImageViewBitmap(@NonNull ImageView view, @DrawableRes int resId,
                                                @IdRes int displayType, int width, int height,
                                                @Nullable OnImageLoadCompleteListener listener) {
        BitmapSource src = new BitmapSource(resId);
        return setImageViewBitmap(view, src, displayType, width, height, listener);
    }

    @SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
    @Nullable
    public static ImageLoaderHandle setImageViewBitmap(@NonNull ImageView view, @NonNull String path,
                                                @IdRes int displayType, int width, int height,
                                                @Nullable OnImageLoadCompleteListener listener) {
        BitmapSource src = new BitmapSource(path);
        return setImageViewBitmap(view, src, displayType, width, height, listener);
    }

    @Nullable
    private static ImageLoaderHandle setImageViewBitmap(@NonNull ImageView view, @NonNull BitmapSource src,
                                                @IdRes int displayType, int width, int height,
                                                @Nullable OnImageLoadCompleteListener listener) {
        if (!mImageTypeLimits.containsKey(displayType)) {
            throw new IllegalArgumentException(String.format(Locale.US, "Unknown image display type / size class: %d.", displayType));
        }


        Log.d(Util.TAG, String.format("Starting image load from cache or resources. Cache holding %d " +
                "active and %d inactive bitmaps, %d free bytes out of %d allowed",
                mCache.getActiveCount(), mCache.getInactiveCount(), mCache.getMaxSize(), mCache.getFreeSpace()));

        // If the correct image is already assigned to the ImageView, do nothing
        if (reassignExistingRBitmapToForeground(view, src, displayType)) return null;

        // Try to get the image from the cache
        if (assignCachedRBitmapToForeground(view, src, displayType)) return null;

        // Need to load from resources. See if it's already loading.
        EmptyReusableBitmap placeholderRBitmap = new EmptyReusableBitmap(src, displayType);
        Pair<ImageLoaderHandle, ImageLoadCompletionItem> resultPair =
                addForegroundCompletionItemToTask(view, listener, placeholderRBitmap);
        ImageLoaderHandle resultHandle = resultPair.first;
        if (resultHandle != null) return resultHandle;

        // Not already loading. Need to start loading.
        return loadNewBitmap(resultPair.second, src, displayType, width, height, placeholderRBitmap);

    }

    // Throws IllegalStateException if the View background was set through BitmapManager methods
    // but then changed to a non-bitmap Drawable without first clearing the old background
    // through BitmapManager methods (or cleared, so there is no Drawable, but not cleared through
    // BitmapManager methods).
    @SuppressWarnings("SameParameterValue")
    @Nullable
    public static ImageLoaderHandle setViewBackground(@NonNull View view, @DrawableRes int resId,
                                                      int gravity, @IdRes int displayType,
                                                      int width, int height,
                                                      @Nullable OnImageLoadCompleteListener listener)
            throws IllegalStateException {
        BitmapSource src = new BitmapSource(resId);
        return setViewBackground(view, src, gravity, displayType, width, height, listener);
    }

    // Throws IllegalStateException if the View background was set through BitmapManager methods
    // but then changed to a non-bitmap Drawable without first clearing the old background
    // through BitmapManager methods (or cleared, so there is no Drawable, but not cleared through
    // BitmapManager methods).
    @SuppressWarnings("unused")
    @Nullable
    public static ImageLoaderHandle setViewBackground(@NonNull View view, @NonNull String path,
                                                      int gravity, @IdRes int displayType,
                                                      int width, int height,
                                                      @Nullable OnImageLoadCompleteListener listener)
            throws IllegalStateException {
        BitmapSource src = new BitmapSource(path);
        return setViewBackground(view, src, gravity, displayType, width, height, listener);
    }

    @Nullable
    private static ImageLoaderHandle setViewBackground(@NonNull View view, @NonNull BitmapSource src,
                                                       int gravity, @IdRes int displayType,
                                                       int width, int height,
                                                       @Nullable OnImageLoadCompleteListener listener)
            throws IllegalStateException {
        Log.d(Util.TAG, String.format("Starting image load from cache or resources. Cache holding %d " +
                        "active and %d inactive bitmaps, %d free bytes out of %d allowed",
                mCache.getActiveCount(), mCache.getInactiveCount(), mCache.getMaxSize(), mCache.getFreeSpace()));

        if (!mImageTypeLimits.containsKey(displayType)) {
            throw new IllegalArgumentException(String.format(Locale.US, "Unknown image display type / size class: %d.", displayType));
        }


        // If the correct image is already assigned to the View, do nothing
        if (reassignExistingRBitmapToBackground(view, src, displayType, gravity)) return null;

        // Try to get the image from the cache
        if (assignCachedRBitmapToBackground(view, src, displayType, gravity)) return null;

        // Need to load from resources. See if it's already loading.
        EmptyReusableBitmap placeholderRBitmap = new EmptyReusableBitmap(src, displayType);
        Pair<ImageLoaderHandle, ImageLoadCompletionItem> resultPair = addBackgroundCompletionItemToTask(view, listener, placeholderRBitmap, gravity);
        ImageLoaderHandle resultHandle = resultPair.first;
        if (resultHandle != null) return resultHandle;

        // Not already loading. Need to start loading.
        return loadNewBitmap(resultPair.second, src, displayType, width, height, placeholderRBitmap);

    }


    @NonNull
    private static ImageLoaderHandle loadNewBitmap(@NonNull ImageLoadCompletionItem completionItem,
                                                   @NonNull BitmapSource src,
                                                   @IdRes int displayType, int width, int height,
                                                   EmptyReusableBitmap placeholderRBitmap) {

        View view = completionItem.isBackgroundItem() ? completionItem.getBackgroundView() : completionItem.getImageView();
        if (view == null) throw new AssertionError();

        List<ImageLoadCompletionItem> completionList = new LinkedList<>();
        completionList.add(completionItem);
        ImageLoaderTask task = new ImageLoaderTask(mOnImageLoadCompleteListener, mCache);
        long maxMem = mCache.getMaxSize() / mImageTypeLimits.get(displayType);
        ImageLoaderTask.ImageLoaderWorkItem workItem = new ImageLoaderTask.ImageLoaderWorkItem(view, src, displayType, width, height, maxMem);
        task.execute(workItem);
        mPendingTasks.put(placeholderRBitmap, new Pair<>(task, completionList));

        Log.d(Util.TAG, "Loading new bitmap from resources");
        return new ImageLoaderHandle(task, completionItem.requestNumber, placeholderRBitmap, mOnImageLoadCompleteListener);
    }

    @NonNull
    private static Pair<ImageLoaderHandle, ImageLoadCompletionItem> addForegroundCompletionItemToTask(@NonNull ImageView view, @Nullable OnImageLoadCompleteListener listener, @NonNull EmptyReusableBitmap placeholderRBitmap) {
        return addCompletionItemToTaskUnsafe(view, listener, placeholderRBitmap, 0, false);
    }

    @NonNull
    private static Pair<ImageLoaderHandle, ImageLoadCompletionItem> addBackgroundCompletionItemToTask(@NonNull View view, @Nullable OnImageLoadCompleteListener listener, @NonNull EmptyReusableBitmap placeholderRBitmap, int gravity) {
        return addCompletionItemToTaskUnsafe(view, listener, placeholderRBitmap, gravity, true);
    }

    // Should not be used directly, since it does an unsafe cast from of the view parameter to
    // an ImageView. Instead, use addBackgroundCompletionItemToTask or addForegroundCompletionItemToTask,
    // which provide type safety.
    @NonNull
    private static Pair<ImageLoaderHandle, ImageLoadCompletionItem> addCompletionItemToTaskUnsafe(@NonNull View view, @Nullable OnImageLoadCompleteListener listener, @NonNull EmptyReusableBitmap placeholderRBitmap, int gravity, boolean isBackground) {
        int requestNumber = nextRequestNumber++;
        ImageLoadCompletionItem resultCompletionItem;
        if (isBackground) {
            resultCompletionItem = ImageLoadCompletionItem.createBackgroundItem(requestNumber, view, listener, gravity);
        } else {
            resultCompletionItem = ImageLoadCompletionItem.createForegroundItem(requestNumber, (ImageView) view, listener);
        }

        ImageLoaderHandle resultHandle = null;
        Pair<ImageLoaderTask, List<ImageLoadCompletionItem>> pendingTask = mPendingTasks.get(placeholderRBitmap);
        if (pendingTask != null) {
            List<ImageLoadCompletionItem> completionList = pendingTask.second;
            if (completionList == null) throw new AssertionError();
            completionList.add(resultCompletionItem);

            Log.d(Util.TAG, "Appended to pending bitmap load, avoided a second load from resources");
            resultHandle = new ImageLoaderHandle(pendingTask.first, requestNumber, placeholderRBitmap, mOnImageLoadCompleteListener);
        }
        return new Pair<>(resultHandle, resultCompletionItem);
    }

    private static boolean assignCachedRBitmapToForeground(@NonNull ImageView view, @NonNull BitmapSource src, @IdRes int displayType) {
        return assignCachedRBitmapUnsafe(view, src, displayType, 0, false);
    }

    private static boolean assignCachedRBitmapToBackground(@NonNull View view, @NonNull BitmapSource src, @IdRes int displayType, int gravity) {
        return assignCachedRBitmapUnsafe(view, src, displayType, gravity, true);
    }

    // Don't use this directly, because it performs an unsafe cast of the view parameter to ImageView.
    // Use assignCachedRBitmapToBackground or assignCachedRBitmapToForeground, which provide type safety
    private static boolean assignCachedRBitmapUnsafe(@NonNull View view, @NonNull BitmapSource src, @IdRes int displayType, int gravity, boolean assignBackground) {
        ReusableBitmap cachedRBitmap = mCache.getBitmap(src, displayType);
        if (cachedRBitmap != null) {
            if (assignBackground) {
                cachedRBitmap.attachToViewBackground(view, gravity);
            } else {
                cachedRBitmap.attachToImageView((ImageView) view);
            }

            // Activating is relatively harmless (constant time) if it's already marked active, so
            // no need to do any checks first.
            mCache.activateBitmap(cachedRBitmap);
            Log.d(Util.TAG, "Used cached bitmap, avoided new load from resources");
            return true;
        }
        return false;
    }

    private static boolean reassignExistingRBitmapToForeground(@NonNull ImageView view, @NonNull BitmapSource src,
                                                               @IdRes int displayType)
            throws IllegalStateException {
        return reassignExistingRBitmapUnsafe(view, src, displayType, 0, false);
    }

    private static boolean reassignExistingRBitmapToBackground(@NonNull View view, @NonNull BitmapSource src,
                                                               @IdRes int displayType, int gravity)
            throws IllegalStateException {
        return reassignExistingRBitmapUnsafe(view, src, displayType, gravity, true);
    }

    // Throws IllegalStateException if the View background was set through BitmapManager methods
    // but then changed to a non-bitmap Drawable without first clearing the old background
    // through BitmapManager methods (or cleared, so there is no Drawable, but not cleared through
    // BitmapManager methods).
    private static boolean reassignExistingRBitmapUnsafe(@NonNull View view, @NonNull BitmapSource src,
                                                         @IdRes int displayType, int gravity, boolean assignBackground)
            throws IllegalStateException {
        ReusableBitmap oldRBitmap = assignBackground ? ReusableBitmap.getBackgroundRBitmap(view) : ReusableBitmap.getForegroundRBitmap((ImageView) view);
        if (oldRBitmap != null && oldRBitmap.src.equals(src) && oldRBitmap.displayType == displayType) {
            if (assignBackground) {
                // Change the gravity if needed
                try {
                    BitmapDrawable drawable = (BitmapDrawable) view.getBackground();
                    if (drawable.getGravity() != gravity) drawable.setGravity(gravity);
                } catch (ClassCastException e) {
                    throw new IllegalStateException("View has tag for ReusableBitmap background but background is not a BitmapDrawable.", e);
                }
            }

            // Activating is relatively harmless (constant time) if it's already marked active, so
            // just be sure it's marked active.
            mCache.activateBitmap(oldRBitmap);
            Log.d(Util.TAG, "Kept bitmap assignment, avoided new load from resources");
            return true;
        }
        return false;
    }

    public static void clearImageViewForeground(ImageView v) {
        ReusableBitmap rBitmap = ReusableBitmap.clearImageViewForeground(v);
        if (rBitmap != null && !rBitmap.isAttached()) {
            mCache.deactivateBitmap(rBitmap);
        }
    }

    public static void clearViewBackground(View v) {
        ReusableBitmap rBitmap = ReusableBitmap.clearViewBackground(v);
        if (rBitmap != null && !rBitmap.isAttached()) {
            mCache.deactivateBitmap(rBitmap);
        }
    }

    public static void clearImageViewForegroundAndBackground(ImageView v) {
        clearImageViewForeground(v);
        clearViewBackground(v);
    }

    static int getBytesPerPixel(Bitmap.Config config) {
        switch (config) {
            case ARGB_8888:
                return 4;
            case ARGB_4444:
                return 2;
            case RGB_565:
                return 2;
            case ALPHA_8:
                return 1;
            default:
                return 0;
        }
    }


    static long getBitmapMemorySizeFromOptions(BitmapFactory.Options options) {
        int effectiveInDensity = options.inDensity != 0 ? options.inDensity : DisplayMetrics.DENSITY_DEFAULT;
        int inDensitySquared = effectiveInDensity * effectiveInDensity;
        int targetDensitySquared = options.inTargetDensity * options.inTargetDensity;

        long sampleSize = options.inSampleSize >= 1 ? options.inSampleSize : 1;
        long sampleSizeSquared = sampleSize * sampleSize;
        long result = (long) options.outWidth * options.outHeight
                * getBytesPerPixel(options.inPreferredConfig)
                / sampleSizeSquared;
        if (options.inScaled && inDensitySquared != 0 && targetDensitySquared != 0) {
            result = result * targetDensitySquared / inDensitySquared;
        }
        return result;
    }


//    private static Bitmap.Config canDowngradeBitmap(Bitmap.Config config) {
//        switch (config) {
//            case ARGB_8888:
//                return Bitmap.Config.RGB_565;
//            case ARGB_4444:
//                return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) ? Bitmap.Config.RGB_565 : null;
//            case RGB_565:
//                return null;
//            case ALPHA_8:
//                return null;
//            default:
//                return null;
//        }
//    }

    private static class ImageLoadCompletionItem {
        public final int requestNumber;
        private final WeakReference<ImageView> mImageViewRef;
        private final WeakReference<View> mBaseViewRef;
        private final WeakReference<OnImageLoadCompleteListener> mListenerRef;
        private final boolean mIsBackground;
        public final int gravity;

        public static ImageLoadCompletionItem createForegroundItem(int requestNumber,
                                                                   @NonNull ImageView imageView,
                                                                   @Nullable OnImageLoadCompleteListener listener) {
            return new ImageLoadCompletionItem(requestNumber, imageView, listener, 0, false);
        }

        public static ImageLoadCompletionItem createBackgroundItem(int requestNumber,
                                                                   @NonNull View view,
                                                                   @Nullable OnImageLoadCompleteListener listener,
                                                                   int gravity) {
            return new ImageLoadCompletionItem(requestNumber, view, listener, gravity, true);
        }

        // Throws ClassCastException if isBackground is false and view is not an ImageView
        private ImageLoadCompletionItem(int requestNumber, @NonNull View view, @Nullable OnImageLoadCompleteListener listener, int gravity, boolean isBackground) {
            this.requestNumber = requestNumber;
            if (isBackground) {
                mBaseViewRef = new WeakReference<>(view);
                mImageViewRef = null;
            } else {
                mImageViewRef = new WeakReference<>((ImageView) view);
                mBaseViewRef = null;
            }
            mListenerRef = new WeakReference<>(listener);
            mIsBackground = isBackground;
            this.gravity = gravity;
        }

        @Nullable
        public ImageView getImageView() {
            return mImageViewRef == null ? null : mImageViewRef.get();
        }

        @Nullable
        public View getBackgroundView() {
            return mBaseViewRef == null ? null : mBaseViewRef.get();
        }

        @Nullable
        public OnImageLoadCompleteListener getListener() {
            return mListenerRef.get();
        }

        public boolean isBackgroundItem() {
            return mIsBackground;
        }
    }

    public interface OnImageLoadCompleteListener {
        void onImageLoadComplete();
    }

    public static class ImageLoaderHandle {
        private final int requestNumber;
        private final WeakReference<ImageLoaderTask> taskRef;
        private final ReusableBitmapAbstract comparisonRBitmap;
        private final ImageLoaderTask.OnImageLoadTaskCompleteListener cancelListener;

        @SuppressWarnings("SameParameterValue")
        private ImageLoaderHandle(@NonNull ImageLoaderTask task, int requestNumber,
                                  ReusableBitmapAbstract comparisonRBitmap,
                                  ImageLoaderTask.OnImageLoadTaskCompleteListener cancelListener) {
            this.requestNumber = requestNumber;
            taskRef = new WeakReference<>(task);
            this.comparisonRBitmap = comparisonRBitmap;
            this.cancelListener = cancelListener;
        }

        public void cancel() {
            cancelListener.onImageLoadCancelled(this);
        }

        @Nullable
        private ImageLoaderTask getTask() {
            return taskRef.get();
        }
    }


}
