package com.erichamion.freelance.oakglen.bm;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.SoftReference;

/**
 * Holds a SoftReference or WeakReference to a ReusableBitmap that is known to be
 * inactive, not used by any Views. The underlying ReusableBitmap could be garbage
 * collected at any time, so its basic identifying information is stored separately.
 * This allows for comparison/identification, as well as for accounting for
 * used/freed memory.
 *
 * Created by Eric Ray on 7/14/2016.
 */
class InactiveBitmapHolder extends ReusableBitmapAbstract {
    private final SoftReference<ReusableBitmap> mRBitmapRef;
    public final BitmapSource src;
    public final int displayType;
    public final int byteSize;

    public InactiveBitmapHolder(@NonNull ReusableBitmap rBitmap) {
        mRBitmapRef = new SoftReference<>(rBitmap);
        displayType = rBitmap.displayType;
        byteSize = rBitmap.byteSize;
        src = rBitmap.src;
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

    @Nullable
    public ReusableBitmap getReusableBitmap() {
        return mRBitmapRef.get();
    }


}
