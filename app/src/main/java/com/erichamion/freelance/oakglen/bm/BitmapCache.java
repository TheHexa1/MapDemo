package com.erichamion.freelance.oakglen.bm;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.erichamion.freelance.oakglen.Util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Maintains a cache of both active and inactive bitmaps. Tries to avoid exceeding
 * the memory limit given in the constructor by pruning unused bitmaps and when
 * necessary by auditing active bitmaps to ensure they are actually active, but
 * will exceed the memory limit if needed to hold all active bitmaps.
 *
 * Created by Eric Ray on 7/14/2016.
 */
class BitmapCache implements ImageLoaderTask.InBitmapSupplier {
    private static final double INBITMAP_SIZE_PAD_FACTOR = 0.01;

    private final long mMaxSize;
    private final Set<ReusableBitmap> mActiveBitmaps = new HashSet<>();
    private final Set<InactiveBitmapHolder> mInactiveBitmaps = new LinkedHashSet<>();

    private long mActiveSize = 0;
    private long mInactiveSize = 0;


    public BitmapCache(long maxSizeBytes) {
        mMaxSize = maxSizeBytes;
    }

    public synchronized long getMaxSize() {
        return mMaxSize;
    }

    public synchronized long getFreeSpace() {
        return mMaxSize - mActiveSize - mInactiveSize;
    }

    public synchronized int getActiveCount() {
        return mActiveBitmaps.size();
    }

    public synchronized int getInactiveCount() {
        return mInactiveBitmaps.size();
    }

    public synchronized ReusableBitmap getBitmap(@NonNull BitmapSource src, @IdRes int displayType) {
        if (!Looper.getMainLooper().equals(Looper.myLooper()))
            throw new AssertionError("Bitmap cache accessed off main thread");
        EmptyReusableBitmap compareBitmap = new EmptyReusableBitmap(src, displayType);
        //noinspection SuspiciousMethodCalls
        if (mActiveBitmaps.contains(compareBitmap)) {
            return getActiveBitmap(src, displayType);
        } else //noinspection SuspiciousMethodCalls
            if (mInactiveBitmaps.contains(compareBitmap)) {
                return getInactiveBitmap(src, displayType);
            }
        return null;
    }

    @Nullable
    private synchronized ReusableBitmap getActiveBitmap(@NonNull BitmapSource src, @IdRes int displayType) {
        if (!Looper.getMainLooper().equals(Looper.myLooper()))
            throw new AssertionError("Bitmap cache accessed off main thread");
        Iterator<ReusableBitmap> iter = mActiveBitmaps.iterator();
        while (iter.hasNext()) {
            ReusableBitmap rBitmap = iter.next();
            if (rBitmap.src.equals(src) && rBitmap.displayType == displayType) {
                return rBitmap;
            }

            // Do partial cleanup while we're going through, but don't fully audit
            // each bitmap's attachment status.
            if (!rBitmap.isAttached()) {
                deactivateBitmap(rBitmap, iter);
            }
        }
        return null;
    }

    @Nullable
    private synchronized ReusableBitmap getInactiveBitmap(@NonNull BitmapSource src, @IdRes int displayType) {
        if (!Looper.getMainLooper().equals(Looper.myLooper()))
            throw new AssertionError("Bitmap cache accessed off main thread");
        Iterator<InactiveBitmapHolder> iter = mInactiveBitmaps.iterator();

        while (iter.hasNext()) {
            InactiveBitmapHolder holder = iter.next();

            // Do partial cleanup while we're going through, and only return a match
            // if it hasn't been cleaned out
            if (holder.getReusableBitmap() == null) {
                iter.remove();
                mInactiveSize -= holder.getByteSize();
            } else if (holder.src.equals(src) && holder.displayType == displayType) {
                return holder.getReusableBitmap();
            }
        }
        return null;
    }

    public synchronized void addBitmap(ReusableBitmap newRBitmap) {
        if (!Looper.getMainLooper().equals(Looper.myLooper()))
            throw new AssertionError("Bitmap cache accessed off main thread");
        if (newRBitmap.isAttached()) {
            addActiveBitmap(newRBitmap);
        } else {
            addInactiveBitmap(newRBitmap);
        }
    }

    public synchronized void activateBitmap(ReusableBitmap rBitmap) {
        activateBitmap(rBitmap, null);
    }

    private synchronized void activateBitmap(@NonNull ReusableBitmap rBitmap, @Nullable Iterator<InactiveBitmapHolder> inactiveIter) {
        if (!Looper.getMainLooper().equals(Looper.myLooper()))
            throw new AssertionError("Bitmap cache accessed off main thread");
        if (mActiveBitmaps.contains(rBitmap)) return;
        // Do a constant-time check before iterating
        //noinspection SuspiciousMethodCalls
        if (!mInactiveBitmaps.contains(rBitmap)) return;

        Iterator<InactiveBitmapHolder> iter;
        ReusableBitmap rBitmapToActivate = null;
        if (inactiveIter == null) {
            iter = mInactiveBitmaps.iterator();
            while (iter.hasNext()) {
                InactiveBitmapHolder currentHolder = iter.next();
                ReusableBitmap currentRBitmap = currentHolder.getReusableBitmap();
                // If we have to iterate through, do some cleanup while we're at it
                if (currentRBitmap == null) {
                    iter.remove();
                    mInactiveSize -= currentHolder.getByteSize();
                    continue;
                }
                if (currentRBitmap.equals(rBitmap)) {
                    if (currentRBitmap != rBitmap)
                        throw new AssertionError("Multiple bitmaps exist for the same Resource ID and display type");
                    rBitmapToActivate = currentRBitmap;
                    break;
                }
            }
        } else {
            iter = inactiveIter;
            rBitmapToActivate = rBitmap;
        }
        if (rBitmapToActivate != null) {
            iter.remove();
            mInactiveSize -= rBitmapToActivate.getByteSize();
            mActiveBitmaps.add(rBitmap);
            mActiveSize += rBitmap.byteSize;
        }

    }

    public synchronized void deactivateBitmap(@NonNull ReusableBitmap rBitmap) {
        deactivateBitmap(rBitmap, null);
    }

    private synchronized void deactivateBitmap(@NonNull ReusableBitmap bitmap, @Nullable Iterator<ReusableBitmap> activeIter) {
        if (!Looper.getMainLooper().equals(Looper.myLooper()))
            throw new AssertionError("Bitmap cache accessed off main thread");
        if (!mActiveBitmaps.contains(bitmap)) throw new AssertionError();
        if (activeIter == null) {
            mActiveBitmaps.remove(bitmap);
        } else {
            activeIter.remove();
        }
        mActiveSize -= bitmap.byteSize;

        mInactiveBitmaps.add(new InactiveBitmapHolder(bitmap));
        mInactiveSize += bitmap.byteSize;
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized boolean trim(long newSizeBytes) {
        if (!Looper.getMainLooper().equals(Looper.myLooper()))
            throw new AssertionError("Bitmap cache accessed off main thread");
        // Check whether we need to do anything
        if (mActiveSize + mInactiveSize <= newSizeBytes) return true;

        if (newSizeBytes < mActiveSize) {
            auditActive();
            mInactiveBitmaps.clear();
            mInactiveSize = 0;
        } else {
            auditInactive();
            Iterator<InactiveBitmapHolder> iter = mInactiveBitmaps.iterator();
            while (mActiveSize + mInactiveSize > newSizeBytes && iter.hasNext()) {
                InactiveBitmapHolder removed = iter.next();
                iter.remove();
                mInactiveSize -= removed.getByteSize();
            }
        }

        return mActiveSize + mInactiveSize <= newSizeBytes;
    }

    private synchronized void addActiveBitmap(ReusableBitmap rBitmap) {
        if (!Looper.getMainLooper().equals(Looper.myLooper()))
            throw new AssertionError("Bitmap cache accessed off main thread");
        boolean trimSuccess = trim(mMaxSize - rBitmap.byteSize);
        if (!trimSuccess) Log.i(Util.TAG, "Bitmap cache size may be exceeded");
        boolean isAdded = mActiveBitmaps.add(rBitmap);
        if (isAdded) {
            mActiveSize += rBitmap.byteSize;
        } else {
            Log.d(Util.TAG, "Did not add duplicate bitmap to active cache");
        }
        if (isAdded && !trimSuccess) {
            String msg = String.format(Locale.US,
                    "Bitmap cache size exceeded by %.2f%% while adding active type %d bitmap: %d active, %d inactive",
                    100.0 * (mInactiveSize + mActiveSize - mMaxSize) / mMaxSize,
                    rBitmap.displayType,
                    mActiveBitmaps.size(),
                    mInactiveBitmaps.size());
            Log.w(Util.TAG, msg);
        }
    }

    private synchronized void addInactiveBitmap(ReusableBitmap rBitmap) {
        if (!Looper.getMainLooper().equals(Looper.myLooper()))
            throw new AssertionError("Bitmap cache accessed off main thread");
        if (!trim(mMaxSize - rBitmap.byteSize)) {
            Log.d(Util.TAG, "Adding inactive bitmap to cache failed, as adding would have exceeded the allowed cache size");
            return;
        }

        boolean success = mInactiveBitmaps.add(new InactiveBitmapHolder(rBitmap));
        if (success) {
            mInactiveSize += rBitmap.byteSize;
        } else {
            Log.d(Util.TAG, "Did not add duplicate bitmap to inactive cache");
        }
    }

    private synchronized void auditActive() {
        if (!Looper.getMainLooper().equals(Looper.myLooper()))
            throw new AssertionError("Bitmap cache accessed off main thread");
        Iterator<ReusableBitmap> iter = mActiveBitmaps.iterator();
        while (iter.hasNext()) {
            ReusableBitmap rBitmap = iter.next();
            if (!rBitmap.isAttachedWithAudit()) {
                iter.remove();
                mActiveSize -= rBitmap.byteSize;
            }
        }
    }

    private synchronized void auditInactive() {
        if (!Looper.getMainLooper().equals(Looper.myLooper()))
            throw new AssertionError("Bitmap cache accessed off main thread");
        Iterator<InactiveBitmapHolder> iter = mInactiveBitmaps.iterator();
        while (iter.hasNext()) {
            InactiveBitmapHolder holder = iter.next();
            if (holder.getReusableBitmap() == null) {
                iter.remove();
                mInactiveSize -= holder.getByteSize();
            } else if (holder.getReusableBitmap().isAttached()) {
                activateBitmap(holder.getReusableBitmap(), iter);
            }
        }
    }

    @Nullable
    @Override
    public synchronized Bitmap getInBitmap(BitmapFactory.Options options) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // Requirements are too stringent on these versions. Just don't
            // bother reusing the memory.
            return null;
        }

        long bytesNeeded = BitmapManager.getBitmapMemorySizeFromOptions(options);
        bytesNeeded += bytesNeeded * INBITMAP_SIZE_PAD_FACTOR;

        Iterator<InactiveBitmapHolder> iter = mInactiveBitmaps.iterator();
        while (iter.hasNext()) {
            InactiveBitmapHolder holder = iter.next();
            ReusableBitmap rBitmap = holder.getReusableBitmap();
            if (rBitmap == null) {
                iter.remove();
                mInactiveSize -= holder.byteSize;
            } else if (rBitmap.isAttached()) {
                activateBitmap(rBitmap, iter);
            } else if (rBitmap.byteSize >= bytesNeeded) {
                iter.remove();
                mInactiveSize -= holder.byteSize;
                return rBitmap.bitmap;
            }
        }
        return null;
    }


}
