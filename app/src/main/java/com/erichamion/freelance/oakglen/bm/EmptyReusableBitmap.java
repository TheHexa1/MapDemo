package com.erichamion.freelance.oakglen.bm;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;

/**
 * Placeholder that can be used for describing or comparing to a ReusableBitmap,
 * without the need to store any actual bitmap.
 *
 * Created by Eric Ray on 7/14/2016.
 */
class EmptyReusableBitmap extends ReusableBitmapAbstract {
    private final BitmapSource src;
    private final int displayType;

    public EmptyReusableBitmap(@NonNull BitmapSource src, @IdRes int displayType) {
        this.src = src;
        this.displayType = displayType;
    }

    @NonNull
    @Override
    public BitmapSource getSrc() {
        return src;
    }

    @Override
    public int getDisplayType() {
        return displayType;
    }

    @Override
    public int getByteSize() {
        return 0;
    }
}
